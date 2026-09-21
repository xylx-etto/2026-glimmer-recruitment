import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Task4 {
    public static void main(String[] args) {
//        List<String> strings = List.of("I", "am", "a", "list", "of", "Strings");
//        Stream<String> stream = strings.stream();
//        Stream<String> limit = stream.limit(4);
//        System.out.println("limit = " + limit);
        List<Student> students = Arrays.asList(
                new Student("Alice", 85),
                new Student("Bob", 58),
                new Student("Charlie", 90),
                new Student("David", 45),
                new Student("Eve", 72),
                new Student("Frank", 60),
                new Student("Grace", 55),
                new Student("Heidi", 95)
        );
        List<String> passingStudents = students.stream().filter(s->s.getScore()>=60).map(Student::getName).sorted().map(String::toUpperCase).collect(Collectors.toList());
        System.out.println(passingStudents);
    }

}
