import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
                .addStaticImport(Collections.class, "singletonList")
                .build();

        javaFile.writeTo(new File("src/test/java"));
        System.out.println("Test file generated: " + testClassName + ".java");
    }

    private static void addFields(TypeSpec.Builder testClass, String className, List<String> dependencies) {
        // Mock fields
        for (String dep : dependencies) {
            testClass.addField(FieldSpec.builder(ClassName.bestGuess(dep), toFieldName(dep))
                    .addAnnotation(Mock.class)
                    .addModifiers(Modifier.PRIVATE)
                    .build());
        }

        // InjectMocks field
        testClass.addField(FieldSpec.builder(ClassName.bestGuess(className), toLowerFirst(className))
                .addAnnotation(InjectMocks.class)
                .addModifiers(Modifier.PRIVATE)
                .build());
    }

    private static void addTestMethods(TypeSpec.Builder testClass, CompilationUnit cu, String className, List<MethodCallInfo> methodCalls) {
        cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isPublic() && !m.isStatic())
                .forEach(method -> {
                    String methodName = method.getNameAsString();
                    MethodSpec testMethod = MethodSpec.methodBuilder("test" + capitalize(methodName))
                            .addAnnotation(Test.class)
                            .addModifiers(Modifier.PUBLIC)
                            .addCode(buildTestBody(method, className, methodCalls))
                            .build();
                    testClass.addMethod(testMethod);
                });
    }

    private static CodeBlock buildTestBody(MethodDeclaration method, String className, List<MethodCallInfo> methodCalls) {
        CodeBlock.Builder code = CodeBlock.builder();

        // Arrange
        code.addStatement("// Arrange");
        List<MethodCallInfo> relevantCalls = methodCalls.stream()
                .filter(mc -> mc.scope.equals(toFieldName(className)) || mc.calledWithin.equals(method.getNameAsString()))
                .collect(Collectors.toList());

        for (MethodCallInfo call : relevantCalls) {
            if (call.methodName.equals("findAll")) {
                code.addStatement ​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​
