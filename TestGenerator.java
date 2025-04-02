package com.example.util;

import com.squareup.javapoet.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@SpringBootTest // Required to load the Spring context if you're testing Spring components
@ExtendWith(MockitoExtension.class) // Required to use Mockito annotations in JUnit 5
public class TestGenerator {

    public static void generateUnitTestsForClass(String className, List<ClassAnalyzer.MethodData> methods, List<ClassAnalyzer.FieldData> fields) throws IOException {
        TypeSpec.Builder testClassBuilder = TypeSpec.classBuilder(className + "Test")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addAnnotation(SpringBootTest.class)
                .addAnnotation(ExtendWith.class)
                .addField(FieldSpec.builder(MockitoAnnotations.class, "mockito", javax.lang.model.element.Modifier.PRIVATE).build());

        // Dynamically mock dependencies
        fields.forEach(field -> {
            FieldSpec mockField = FieldSpec.builder(ClassName.get(field.fieldType), field.fieldName, javax.lang.model.element.Modifier.PRIVATE, javax.lang.model.element.Modifier.MOCK)
                    .build();
            testClassBuilder.addField(mockField);
        });

        // Add the class under test
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

            // Add mock parameters dynamically
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    methodArgs.add(", ");
                }
                methodArgs.add("$L", "mockParam" + i);
            }

            MethodSpec.Builder testMethodBuilder = MethodSpec.methodBuilder("test" + capitalize(methodName))
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .addAnnotation(Test.class) // JUnit 5 annotation
                    .addCode(methodArgs.build())
                    .returns(void.class)
                    .addStatement("$L", methodName);

            testClassBuilder.addMethod(testMethodBuilder.build());
        }

        // Build the generated class
        JavaFile javaFile = JavaFile.builder("com.example.generated", testClassBuilder.build())
                .build();

        // Write the generated test class to the file system
        javaFile.writeTo(Paths.get("./src/test/java"));
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
