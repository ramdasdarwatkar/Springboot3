import org.objectweb.asm.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    // This method analyzes the class file and extracts method signatures, return types, and parameters
    public static List<MethodData> analyzeClass(String classFilePath) throws IOException {
        List<MethodData> methods = new ArrayList<>();

        // Load the class file
        FileInputStream fis = new FileInputStream(classFilePath);
        ClassReader classReader = new ClassReader(fis);

        // Create a ClassVisitor to visit methods
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                // Extract method return type and parameter types from the descriptor
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

        return methods;
    }

    public static void main(String[] args) throws IOException {
        // Example usage: Analyze the class and print methods
        List<MethodData> methods = analyzeClass("path/to/YourClass.class");

        // Print method information
        for (MethodData method : methods) {
            System.out.println(method);  // Display method signature and return type
        }
    }
}
