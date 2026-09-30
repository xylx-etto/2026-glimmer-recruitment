package game;

public class CharacterFactory {
    // TODO: 实现工厂方法，根据角色类型返回对应的角色对象
    // 提示：如果传入未知类型，可以抛出 IllegalArgumentException
    public Character getCharacter(String characterType){
        return switch (characterType) {
            case "Saber" -> new Saber();
            case "Archer" -> new Archer();
            case "Caster" -> new Caster();
            default -> throw new IllegalArgumentException("未定义的角色");
        };
    }
}