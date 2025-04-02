import com.squareup.javapoet.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TestGenerator {

    // This method will generate unit tests dynamically for any Java class
    public static void generateUnitTestsForClass(String className, List<ClassAnalyzer.MethodData> methods, List<ClassAnalyzer.FieldData> fields) throws IOException {
        // Create the test class with the same name as the class being tested (e.g., YourClassTest)
        TypeSpec.Builder testClassBuilder = TypeSpec.classBuilder(className + "Test")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addAnnotation(SpringBootTest.class)
                .addAnnotation(org.junit.jupiter.api.extension.ExtendWith.class)
                .addField(FieldSpec.builder(MockitoAnnotations.class, "mockito", javax.lang.model.element.Modifier.PRIVATE).build());

        // Dynamically mock the dependencies (fields in the original class)
        fields.forEach(field -> {
            FieldSpec mockField = FieldSpec.builder(ClassName.get(field.fieldType), field.fieldName, javax.lang.model.element.Modifier.PRIVATE, javax.lang.model.element.Modifier.MOCK)
                    .build();
            testClassBuilder.addField(mockField);
        });

        // Add a field for the class being tested
        FieldSpec classField = FieldSpec.builder(ClassName.get("com.example", className), "classUnderTest", javax.lang.model.element.Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build();
        testClassBuilder.addField(classField);

        // Constructor to initialize mocks
        testClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addStatement("MockitoAnnotations.openMocks(this)")
                .build());

        // Loop over methods and generate test methods dynamically
        for (ClassAnalyzer.MethodData method : methods) {
            String methodName = method.name;
            String returnType = method.returnType;
            List<String> parameters = method.parameters;

            CodeBlock.Builder methodArgs = CodeBlock.builder();

            // Mock method arguments dynamically
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    methodArgs.add(", ");
                }
                methodArgs.add("$L", "mockParam" + i);  // Example: Create mock params dynamically
            }

            // Generate the test method dynamically
            MethodSpec.Builder testMethodBuilder = MethodSpec.methodBuilder("test" + capitalize(methodName))
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .addAnnotation(Test.class)
                    .returns(void.class)
                    .addStatement("System.out.println(\"Testing " + methodName + "\")");

            // Handle void methods
            if ("void".equals(returnType)) {
                testMethodBuilder.addStatement("classUnderTest.$L($L)", methodName, methodArgs.build());
                testMethodBuilder.addStatement("assertTrue(true)");  // Placeholder for void methods
            } else {
                // For non-void methods, assert the result
                testMethodBuilder.addStatement("$T result = classUnderTest.$L($L)", ClassName.get(returnType), methodName, methodArgs.build());
                testMethodBuilder.addStatement("assertNotNull(result)");  // Assert that result is not null
            }

            // Add the test method to the test class
            testClassBuilder.addMethod(testMethodBuilder.build());
        }

        // Build the test class
        TypeSpec testClass = testClassBuilder.build();
        JavaFile javaFile = JavaFile.builder("com.example.tests", testClass)
                .build();

        // Write the generated test class to the file system
        javaFile.writeTo(Paths.get("./src/test/java"));
    }

    // Helper method to capitalize the first letter of a method name
    private static String capitalize(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    public static void main(String[] args) throws IOException {
        // Example usage: Analyze the class dynamically and generate tests for any class

        // Dynamically analyze a class (e.g., YourClass)
        String classFilePath = "path/to/YourClass.class";
        Map<String, Object> classData = ClassAnalyzer.analyzeClass(classFilePath);

        List<ClassAnalyzer.MethodData> methods = (List<ClassAnalyzer.MethodData>) classData.get("methods");
        List<ClassAnalyzer.FieldData> fields = (List<ClassAnalyzer.FieldData>) classData.get("fields");

        // Generate unit tests dynamically based on the analyzed class
        generateUnitTestsForClass("YourClass", methods, fields);
    }
}
