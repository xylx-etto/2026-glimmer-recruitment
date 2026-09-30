import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Test  {
    static void main (String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        for (Integer i : list) {
            System.out.println(i);
        }
        list.forEach(integer -> {
            System.out.println(integer);
            return;
        });

    }
}
