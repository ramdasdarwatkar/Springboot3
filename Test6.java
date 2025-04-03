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
import java.util.List;
import java.util.stream.Collectors;

public class TestGenerator {

    public static void generateTestForFile(String sourceFilePath) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(new File(sourceFilePath));
        String className = cu.getPrimaryTypeName().get();
        String testClassName = className + "Test";

        DependencyCollector collector = new DependencyCollector();
        cu.accept(collector, null);

        TypeSpec.Builder testClass = TypeSpec.classBuilder(testClassName)
                .addAnnotation(ClassName.get("org.mockito.junit.jupiter", "MockitoExtension"))
                .addModifiers(Modifier.PUBLIC);

        addFields(testClass, className, collector.dependencies);
        addTestMethods(testClass, cu, className, collector.methodCalls, collector.dependencies, collector.exceptionMethods);

        JavaFile javaFile = JavaFile.builder("", testClass.build())
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
        cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isPublic() && !m.isStatic())
                .forEach(method -> {
                    testClass.addMethod(buildTestMethod(method, className, methodCalls, dependencies, exceptionMethods));
                });
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
        if (exceptionMethods.contains(methodName)) {
            code.beginControlFlow("try");
            code.addStatement("$L result = $L.$L()", method.getTypeAsString(), toLowerFirst(className), methodName);
            code.addStatement("assert result != null");
            code.nextControlFlow("catch (Exception e)");
            code.addStatement("assert e != null");
            code.endControlFlow();
        } else {
            code.addStatement("$L result = $L.$L()", method.getTypeAsString(), toLowerFirst(className), methodName);
            code.addStatement("// Assert");
            code.addStatement("assert result != null");
        }

        return testMethod.addCode(code.build()).build();
    }

    private static void generateMockSetup(CodeBlock.Builder code, MethodCallInfo call) {
        if (!call.scope.isEmpty() && !call.scope.equals("this")) {
            code.addStatement("$T.when($L.$L($L)).thenReturn($T.any())",
                    ClassName.get("org.mockito", "Mockito"), toFieldName(call.scope), call.methodName,
                    call.args.isEmpty() ? "" : call.args,
                    Object.class);
        }
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
        List<String> exceptionMethods = new ArrayList<>();

        @Override
        public void visit(FieldDeclaration n, Void arg) {
            String type = n.getVariables().get(0).getTypeAsString();
            dependencies.add(type);
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            String scope = n.getScope().map(Object::toString).orElse("this");
            String methodName = n.getNameAsString();
            String calledWithin = n.findAncestor(MethodDeclaration.class)
                    .map(m -> m.getNameAsString()).orElse("");
            methodCalls.add(new MethodCallInfo(scope, methodName, calledWithin));
            super.visit(n, arg);
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            String methodName = n.findAncestor(MethodDeclaration.class)
                    .map(m -> m.getNameAsString()).orElse("");
            if (!exceptionMethods.contains(methodName)) {
                exceptionMethods.add(methodName);
            }
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
