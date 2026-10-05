import java.io.*;

public class Main {
    static void main(String[] args) {
        Student student = new Student(1, "doro", 2, "114514");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("student.dat"));){
            oos.writeObject(student);
        }catch (IOException e){
            throw new RuntimeException(e);
        }

        try(ObjectInputStream ois =new ObjectInputStream(new FileInputStream("student.dat"))){
            Student s= (Student) ois.readObject();
            System.out.println(s);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
