package Repository;

public class Main {


    static public void main(String[] args) {
        MyRepository<Object> myRepository=new MyRepository<>();
        User user1=new User("name",10);
        myRepository.insert(user1);
        String str="string";
        myRepository.insert(str);
        Integer i=1;
        myRepository.insert(i);

        myRepository.foreach();

    }
}
