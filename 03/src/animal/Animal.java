package animal;

public class Animal {
    protected String name;
    protected int id;
    public Animal(String myName, int myId) {
        name = myName;
        id = myId;
    }
    public void introduction() {
        System.out.println("大家好！我是" + id + "号" + name + "。");
    }
}