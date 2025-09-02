package org.example.tgservice.Enum;

public enum Role {
    MECHANIC("Автомеханік"),
    AUTO_ELECTRICIAN("Автоелектрик"),
    DIAGNOSTIC("Діагност"),
    ENGINE_SPECIALIST("Моторист"),
    TRANSMISSION_SPECIALIST("Трансмісіонщик"),
    SUSPENSION_SPECIALIST("Ходовик"),
    CAR_REPAIR_LOCKSMITH("Слюсар з ремонту авто"),
    TIRE_FITTER("Шиномонтажник"),
    GAS_INSTALLER("Газовщик (ГБО)"),
    PAINTER("Маляр"),
    BODY_SPECIALIST("Кузовщик"),
    SHEET_METAL_WORKER("Жестянщик"),
    WELDER("Сварщик"),
    POLISHER("Поліровщик"),
    SERVICE_RECEPTIONIST("Майстер-приймальник"),
    SERVICE_MANAGER("Сервіс-менеджер"),
    WORKSHOP_MANAGER("Керівник СТО"),
    ADMINISTRATOR("Адміністратор"),
    CAR_WASHER("Мийник"),
    DAMAGE_ASSESSOR("Оцінювач збитків"),
    PARTS_MANAGER("Запчастинник");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromString(String text) {
        for (Role role : Role.values()) {
            if (role.getLabel().equalsIgnoreCase(text) ||
                    role.name().equalsIgnoreCase(text)) {
                return role;
            }
        }
        return null;
    }
}
