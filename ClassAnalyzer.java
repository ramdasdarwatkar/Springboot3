import org.objectweb.asm.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ClassAnalyzer {

    // Class to hold method data (name, return type, parameters)
    public static class MethodData {
        String name;
        String returnType;
        List<String> parameters;

        public MethodData(String name, String returnType, List<String> parameters) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
        }

        @Override
        public String toString() {
            return "MethodData{name='" + name + "', returnType='" + returnType + "', parameters=" + parameters + "}";
        }
    }

    // Class to hold field data (field name and field type)
    public static class FieldData {
        String fieldName;
        String fieldType;

        public FieldData(String fieldName, String fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        @Override
        public String toString() {
            return "FieldData{fieldName='" + fieldName + "', fieldType='" + fieldType + "'}";
        }
    }

    // This method analyzes the class file and extracts method signatures, return types, parameters, and field data
    public static Map<String, Object> analyzeClass(String classFilePath) throws IOException {
        List<MethodData> methods = new ArrayList<>();
        List<FieldData> fields = new ArrayList<>();

        // Load the class file
        FileInputStream fis = new FileInputStream(classFilePath);
        ClassReader classReader = new ClassReader(fis);

        // Create a ClassVisitor to visit methods and fields
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                String fieldType = Type.getType(descriptor).getClassName();
                fields.add(new FieldData(name, fieldType));
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                String returnType = Type.getReturnType(descriptor).getClassName();
                List<String> parameterTypes = new ArrayList<>();
                Type[] argTypes = Type.getArgumentTypes(descriptor);
                for (Type type : argTypes) {
                    parameterTypes.add(type.getClassName());
                }

                // Create a MethodData object for the method
                methods.add(new MethodData(name, returnType, parameterTypes));
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("methods", methods);
        result.put("fields", fields);
        return result;
    }

    public static void main(String[] args) throws IOException {
        // Example usage: Analyze the class and print methods and fields
        Map<String, Object> classData = analyzeClass("path/to/YourClass.class");

        List<MethodData> methods = (List<MethodData>) classData.get("methods");
        List<FieldData> fields = (List<FieldData>) classData.get("fields");

        // Print method and field information
        System.out.println("Methods: " + methods);
        System.out.println("Fields: " + fields);
    }
}
