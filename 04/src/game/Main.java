package game;

public class Main {
    public static void main(String[] args) {
        // TODO:
        // 1. 通过工厂分别创建 Saber、Archer、Caster 三个角色
        // 2. 让每个角色发动攻击（调用 attack）
        // 期待输出格式参考如下：
        // [剑士] 阿尔托莉雅 使用 Excalibur 发动攻击！
        // ...
        CharacterFactory characterFactory = new CharacterFactory();
        Character saber = characterFactory.getCharacter("Saber");
        Character archer = characterFactory.getCharacter("Archer");
        Character caster = characterFactory.getCharacter("Caster");
        saber.attack();
        archer.attack();
        caster.attack();

    }
}