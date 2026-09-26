package tool;

public class Person {
    private String name;
    int age;
    String gender;
    public static int num=0;

    Person(String name,int age,String gender){
        this.name=name;
        this.age=age;
        this.gender=gender;
        num++;
    }
    public Person(){

    }
    public Person(Person object){
        this.name=object.name;
        this.age=object.age;
        this.gender=object.gender;
    }
    public void modify(String name){
        this.name=name;
        System.out.println(this.name);
    }
    public static void eat(){
        System.out.println("I'm hungry");
    }
}
