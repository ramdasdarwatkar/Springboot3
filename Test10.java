import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestGenerator {

    public static void generateTestForFile(String sourceFilePath) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(new File(sourceFilePath));
        String className = cu.getPrimaryTypeName().orElse("UnknownClass");
        String testClassName = className + "Test";

        DependencyCollector collector = new DependencyCollector();
        cu.accept(collector, null);

        TypeSpec.Builder testClass = TypeSpec.classBuilder(testClassName)
                .addAnnotation(ClassName.get("org.junit.jupiter.api.extension", "ExtendWith"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.mockito.junit.jupiter", "MockitoExtension")).build())
                .addModifiers(Modifier.PUBLIC);

        addFields(testClass, className, collector.dependencies);
        addTestMethods(testClass, cu, className, collector.methodCalls, collector.dependencies, collector.exceptionMethods);

        JavaFile javaFile = JavaFile.builder("com.example", testClass.build())
                .build();

        javaFile.writeTo(new File("src/test/java"));
        System.out.println("Test file generated: " + testClassName + ".java");
    }

    private static void addFields(TypeSpec.Builder testClass, String className, List<String> dependencies) {
        for (String dep : dependencies) {
            testClass.addField(FieldSpec.builder(ClassName.bestGuess(dep), toFieldName(dep))
                    .addAnnotation(ClassName.get("org.mockito", "Mock"))
                    .addModifiers(Modifier.PRIVATE)
                    .build());
        }
        testClass.addField(FieldSpec.builder(ClassName.bestGuess(className), toLowerFirst(className))
                .addAnnotation(ClassName.get("org.mockito", "InjectMocks"))
                .addModifiers(Modifier.PRIVATE)
                .build());
    }

    private static void addTestMethods(TypeSpec.Builder testClass, CompilationUnit cu, String className, List<MethodCallInfo> methodCalls, List<String> dependencies, List<String> exceptionMethods) {
        cu.findAll(MethodDeclaration.class).forEach(method ->
            testClass.addMethod(buildTestMethod(method, className, methodCalls, dependencies, exceptionMethods))
        );
    }

    private static MethodSpec buildTestMethod(MethodDeclaration method, String className, List<MethodCallInfo> methodCalls, List<String> dependencies, List<String> exceptionMethods) {
        String methodName = method.getNameAsString();
        MethodSpec.Builder testMethod = MethodSpec.methodBuilder("test" + capitalize(methodName))
                .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                .addModifiers(Modifier.PUBLIC);

        CodeBlock.Builder code = CodeBlock.builder();
        code.addStatement("// Arrange");

        methodCalls.stream()
                .filter(mc -> mc.calledWithin.equals(methodName) && dependencies.contains(mc.scope))
                .forEach(mc -> generateMockSetup(code, mc));

        code.addStatement("// Act");
        code.addStatement("Object result = $L.$L()", toLowerFirst(className), methodName);
        
        code.addStatement("// Assert");
        code.addStatement("assertNotNull(result)");

        methodCalls.stream()
                .filter(mc -> mc.calledWithin.equals(methodName) && dependencies.contains(mc.scope))
                .forEach(mc -> code.addStatement("verify($L, times(1)).$L($L)", toFieldName(mc.scope), mc.methodName, generateMockArguments(mc.args)));

        return testMethod.addCode(code.build()).build();
    }

    private static void generateMockSetup(CodeBlock.Builder code, MethodCallInfo call) {
        if (!call.scope.isEmpty() && !call.scope.equals("this")) {
            String argsPlaceholder = generateMockArguments(call.args);
            String mockReturnValue;
            switch (call.returnType) {
                case "int": mockReturnValue = "10"; break;
                case "double": mockReturnValue = "10.0"; break;
                case "boolean": mockReturnValue = "true"; break;
                case "String": mockReturnValue = "\"MockValue\""; break;
                case "void":
                    code.addStatement("doNothing().when($L).$L($L)", toFieldName(call.scope), call.methodName, argsPlaceholder);
                    return;
                default:
                    mockReturnValue = "Mockito.mock(" + call.returnType + ".class)";
            }
            code.addStatement("when($L.$L($L)).thenReturn($L)", toFieldName(call.scope), call.methodName, argsPlaceholder, mockReturnValue);
        }
    }

    private static String generateMockArguments(String methodSignature) {
        String[] params = methodSignature.split(",");
        if (params.length == 0 || params[0].trim().isEmpty()) return "";

        return Arrays.stream(params)
                .map(param -> {
                    param = param.trim();
                    if (param.contains("String")) return "Mockito.anyString()";
                    if (param.contains("int")) return "Mockito.anyInt()";
                    if (param.contains("double")) return "Mockito.anyDouble()";
                    if (param.contains("boolean")) return "Mockito.anyBoolean()";
                    return "Mockito.any(" + param + ".class)";
                })
                .collect(Collectors.joining(", "));
    }

    public static void main(String[] args) throws IOException {
        generateTestForFile("src/main/java/UserService.java");
    }
}
