import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestGenerator {

    public static void generateTestForFile(String sourceFilePath) throws IOException {
        // Parse the source file
        CompilationUnit cu = StaticJavaParser.parse(new File(sourceFilePath));
        String className = cu.getPrimaryTypeName().get();
        String testClassName = className + "Test";

        // Collect dependencies and method calls
        DependencyCollector collector = new DependencyCollector();
        cu.accept(collector, null);

        // Build test class with JavaPoet
        TypeSpec.Builder testClass = TypeSpec.classBuilder(testClassName)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.mockito.junit.jupiter", "MockitoExtension"))
                        .buildAnnotation("ExtendWith"))
                .addModifiers(Modifier.PUBLIC);

        // Add fields
        addFields(testClass, className, collector.dependencies);

        // Add test methods
        addTestMethods(testClass, cu, className, collector.methodCalls);

        // Generate Java file
        JavaFile javaFile = JavaFile.builder("", testClass.build())
                .addStaticImport(Collections.class, "singletonList", "emptyList")
                .build();

        javaFile.writeTo(new File("src/test/java"));
        System.out.println("Test file generated: " + testClassName + ".java");
    }

    private static void addFields(TypeSpec.Builder testClass, String className, List<String> dependencies) {
        for (String dep : dependencies) {
            testClass.addField(FieldSpec.builder(ClassName.bestGuess(dep), toFieldName(dep))
                    .addAnnotation(Mock.class)
                    .addModifiers(Modifier.PRIVATE)
                    .build());
        }
        testClass.addField(FieldSpec.builder(ClassName.bestGuess(className), toLowerFirst(className))
                .addAnnotation(InjectMocks.class)
                .addModifiers(Modifier.PRIVATE)
                .build());
    }

    private static void addTestMethods(TypeSpec.Builder testClass, CompilationUnit cu, String className, List<MethodCallInfo> methodCalls) {
        cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isPublic() && !m.isStatic())
                .forEach(method -> {
                    // Happy path test
                    testClass.addMethod(buildTestMethod(method, className, methodCalls, "HappyPath", false));
                    // Exception test
                    testClass.addMethod(buildTestMethod(method, className, methodCalls, "Exception", true));
                });
    }

    private static MethodSpec buildTestMethod(MethodDeclaration method, String className, List<MethodCallInfo> methodCalls, String suffix, boolean testException) {
        String methodName = method.getNameAsString();
        MethodSpec.Builder testMethod = MethodSpec.methodBuilder("test" + capitalize(methodName) + suffix)
                .addAnnotation(Test.class)
                .addModifiers(Modifier.PUBLIC);

        // Parameters
        List<Parameter> params = method.getParameters();
        CodeBlock.Builder code = CodeBlock.builder();

        // Arrange
        code.addStatement("// Arrange");
        List<MethodCallInfo> relevantCalls = methodCalls.stream()
                .filter(mc -> mc.scope.equals(toFieldName(className)) || mc.calledWithin.equals(methodName))
                .collect(Collectors.toList());

        // Handle parameters and conditionals
        for (Parameter param : params) {
            String paramName = param.getNameAsString();
            String paramType = param.getTypeAsString();
            if (paramType.equals("String")) {
                code.addStatement("$T $L = $S", String.class, paramName, testException ? "" : "testFilter");
            }
        }

        // Mock setup with return type inference
        for (MethodCallInfo call : relevantCalls) {
            String mockVar = generateMockSetup(code, call, testException);
            if (call.methodName.equals("someMethod") && testException) {
                code.addStatement("$T.when($L.$L($L)).thenThrow(new $T($S))", 
                        Mockito.class, call.scope, call.methodName, "1", RuntimeException.class, "Test exception");
            } else if (mockVar != null) {
                code.addStatement("$T.when($L.$L($L)).thenReturn($L)", 
                        Mockito.class, call.scope, call.methodName, call.args.isEmpty() ? "" : "testFilter", mockVar);
            }
        }

        // Act
        code.addStatement("// Act");
        String returnType = method.getTypeAsString();
        String paramArgs = params.stream().map(Parameter::getNameAsString).collect(Collectors.joining(", "));
        code.addStatement("$L result = $L.$L($L)", returnType, toLowerFirst(className), methodName, paramArgs);

        // Assert
        code.addStatement("// Assert");
        code.addStatement("assert result != null");
        if (testException) {
            code.addStatement("assert result.isEmpty()");
        } else {
            code.addStatement("assert !result.isEmpty()");
        }
        for (MethodCallInfo call : relevantCalls) {
            code.addStatement("$T.verify($L).$L($L)", Mockito.class, call.scope, call.methodName, call.args.isEmpty() ? "" : "testFilter");
        }

        return testMethod.addCode(code.build()).build();
    }

    private static String generateMockSetup(CodeBlock.Builder code, MethodCallInfo call, boolean testException) {
        if (call.methodName.equals("findAll")) {
            code.addStatement("$T<User> mockUsers = singletonList(new User())", List.class);
            return "mockUsers";
        } else if (call.methodName.equals("findByFilter")) {
            code.addStatement("$T<User> mockUsers = singletonList(new User())", List.class);
            call.args = "testFilter"; // Track argument for verification
            return "mockUsers";
        } else if (call.methodName.equals("someMethod") && !testException) {
            call.args = "1"; // Assuming int parameter based on users.size()
            return null; // void method
        }
        return null;
    }

    private static String toFieldName(String className) {
        return toLowerFirst(className);
    }

    private static String toLowerFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static class DependencyCollector extends VoidVisitorAdapter<Void> {
        List<String> dependencies = new ArrayList<>();
        List<MethodCallInfo> methodCalls = new ArrayList<>();

        @Override
        public void visit(FieldDeclaration n, Void arg) {
            if (n.getModifiers().stream().anyMatch(m -> m.getKeyword().asString().equals("final"))) {
                String type = n.getVariables().get(0).getTypeAsString();
                if (type.endsWith("Repository")) {
                    dependencies.add(type);
                }
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            String scope = n.getScope().map(s -> s.toString()).orElse("");
            String methodName = n.getNameAsString();
            String calledWithin = n.findAncestor(MethodDeclaration.class)
                    .map(m -> m.getNameAsString()).orElse("");
            methodCalls.add(new MethodCallInfo(scope, methodName, calledWithin));
            super.visit(n, arg);
        }
    }

    static class MethodCallInfo {
        String scope;
        String methodName;
        String calledWithin;
        String args = "";

        MethodCallInfo(String scope, String methodName, String calledWithin) {
            this.scope = scope;
            this.methodName = methodName;
            this.calledWithin = calledWithin;
        }
    }

    public static void main(String[] args) throws IOException {
        generateTestForFile("src/main/java/UserService.java");
    }
}
