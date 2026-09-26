package animal;

//import static tool.Person.num;

public class Main {
    public static void main(String[] args){
//        Person p1=new Person(); Person.num++;
//        p1.name=null;
//        p1.age= 10;
//        p1.gender="unknown";
//        Person p2=new Person(p1);
//        System.out.println(p2.name+p2.age+p2.gender);
//        p1.modify("name");
//        Person.eat();
//        System.out.println(Person.num);

        //Animals
        Animal animal1=new Penguin("a",1);
        Penguin animal2=new Penguin("b",2);
        System.out.println(animal1.id);
        System.out.println(animal2.id);
//        animal1.introduction();
//        animal2.introduction();
    }

}
