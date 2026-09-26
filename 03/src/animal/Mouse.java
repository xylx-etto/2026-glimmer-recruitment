package animal;

public class Mouse extends Animal {
    public Mouse(String b, int i) {
        super(b,i);
    }
    @Override
    public void introduction(){
        super.introduction();
        System.out.println("大家好！我是" + id + "号老鼠" + name + "。");
    }
}
