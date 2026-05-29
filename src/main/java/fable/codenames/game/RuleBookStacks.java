package fable.codenames.game;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

public final class RuleBookStacks {

    private static final Identifier LIGHT_SCHOLAR_BOOK_ID = new Identifier("scholar", "light_blue_written_book");
    private static final Identifier DARK_SCHOLAR_BOOK_ID = new Identifier("scholar", "cyan_written_book");
    private static final String BOOK_KEY = "CodenamesRuleBook";
    private static final String AUTHOR = "Fable Unity";

    private static final String BLUE = "#42aaff";  // light blue ✅
    private static final String CYAN = "#7ee8e8";  // cyan ✅
    private static final String RED = "#ff3030";
    private static final String DARK_RED = "#aa0000";
    private static final String GOLD = "#ffd35a";
    private static final String ACCENT = "#5a422f";

    private RuleBookStacks() {
    }

    public static ItemStack shortRules() {
        return create("short", "Краткие правила", AUTHOR, SHORT_RULES);
    }

    public static ItemStack fullRules() {
        return create("full", "Подробные правила", AUTHOR, FULL_RULES);
    }

    public static boolean isRuleBook(ItemStack stack, String type) {
        if (stack.isEmpty() || !stack.hasNbt())
            return false;
        return type.equals(stack.getNbt().getString(BOOK_KEY));
    }

    public static boolean isAnyRuleBook(ItemStack stack) {
        return isRuleBook(stack, "short") || isRuleBook(stack, "full");
    }

    private static ItemStack create(String type, String title, String author, String[] pages) {
        ItemStack stack = new ItemStack(selectBookItemByTitle(title));

        TextColor nameColor = switch (type) {
            case "short" -> TextColor.parse(CYAN);
            case "full"  -> TextColor.parse(BLUE);
            default      -> TextColor.fromRgb(0xFFFFFF);
        };

        stack.setCustomName(
                Text.literal(title)
                        .setStyle(Style.EMPTY
                                .withColor(nameColor)
                                .withItalic(false))
        );

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(BOOK_KEY, type);
        nbt.putString("title", title);
        nbt.putString("author", author);
        nbt.putBoolean("resolved", true);

        NbtList pagesNbt = new NbtList();
        for (String pageJson : pages) {
            pagesNbt.add(NbtString.of(pageJson));
        }

        nbt.put("pages", pagesNbt);
        return stack;
    }


    private static Item selectBookItemByTitle(String title) {
        String normalized = title.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("краткие")) {
            return scholarOrVanilla(LIGHT_SCHOLAR_BOOK_ID);
        }
        if (normalized.contains("подробные")) {
            return scholarOrVanilla(DARK_SCHOLAR_BOOK_ID);
        }
        return Items.WRITTEN_BOOK;
    }

    private static Item scholarOrVanilla(Identifier id) {
        Item scholarItem = Registries.ITEM.get(id);
        return scholarItem != Items.AIR ? scholarItem : Items.WRITTEN_BOOK;
    }
    private static String page(Segment... segments) {
        StringBuilder builder = new StringBuilder("{\"text\":\"\",\"extra\":[");
        for (int i = 0; i < segments.length; i++) {
            if (i > 0)
                builder.append(',');
            segments[i].appendJson(builder);
        }
        return builder.append("]}").toString();
    }

    private static Segment t(String text) {
        return new Segment(text, false, null, false);
    }

    private static Segment b(String text) {
        return new Segment(text, true, ACCENT, false);
    }

    private static Segment c(String text, String color) {
        return new Segment(text, false, color, false);
    }

    private static Segment cb(String text, String color) {
        return new Segment(text, true, color, false);
    }

    private static Segment i(String text) {
        return new Segment(text, false, null, true);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private record Segment(String text, boolean bold, String color, boolean italic) {
        private void appendJson(StringBuilder builder) {
            builder.append("{\"text\":\"")
                    .append(escape(this.text))
                    .append("\"")
                    .append(",\"font\":\"minecraft:uniform\""); // 👈 Unicode ТОЛЬКО В КНИГЕ

            if (this.bold)
                builder.append(",\"bold\":true");
            if (this.italic)
                builder.append(",\"italic\":true");
            if (this.color != null)
                builder.append(",\"color\":\"").append(this.color).append("\"");

            builder.append("}");
        }
    }

    // --- SHORT RULES (без изменений текста) ---
    private static final String[] SHORT_RULES = {
            page(
                    cb("Правила, вкратце\n", CYAN),
                    b("Таймер\n"),
                    t("Лидер пишет в чат "),
                    b("одно слово"),
                    t(" и число: сколько объектов связано с подсказкой.\n"),
                    t("После этого команда обсуждает "),
                    b("1 минуту"),
                    t(" и выбирает объекты.\n\n"),
                    b("О подсказках\n"),
                    t("• строго одно слово\n• любая часть речи\n• после слова обязательно число\n"),
                    b("Пример: "),
                    t("КРИПЕР + БЛОК → "),
                    cb("Зелёный 2\n\n", BLUE),
                    t("Можно давать на 1 объект, но 3–4 одной подсказкой — успех.")),
            page(
                    b("Запрещено\n"),
                    t("• "),
                    b("однокоренные слова\n"),
                    t("• части сложных слов: если есть СУПЕРГЕРОЙ, нельзя «герой», «супер»\n"),
                    t("• намёки на буквы, расположение, форму или место на стене\n\n"),
                    b("Разрешено\n"),
                    t("• числа, если они связаны "),
                    b("по смыслу\n"),
                    t("  "),
                    i("«Восемь 3» для ШАР, НОМЕР, ОСЬМИНОГ\n"),
                    t("• реальные русские слова\n• аббревиатуры: СПб, вуз, лазер\n• имена собственные по договорённости\n• рифмы только по смыслу")),
            page(
                    b("Продвинутые подсказки\n"),
                    cb("◆ «0»\n", BLUE),
                    t("Ни один объект не относится к слову. Команда всё равно должна выбрать "),
                    b("минимум один объект"),
                    t(", но может выбрать сколько угодно.\n\n"),
                    cb("◆ «Неограниченно»\n", BLUE),
                    t("Можно сказать вместо числа, чтобы доразгадать старые подсказки.\n"),
                    t("Минус — игроки не знают лимит.\nПлюс — "),
                    b("нет лимита попыток.")),
            page(
                    b("Оспаривание\n"),
                    t("У игроков есть предмет "),
                    b("«!»"),
                    t(". Если хотя бы один игрок из каждой команды нажмёт его, подсказка оспаривается.\n\n"),
                    c("! Подсказка была оспорена командой <>\n", RED),
                    c("! Участник из команды соперников должен подтвердить спор за 30 секунд\n\n", RED),
                    t("Если подсказка некорректна:\n• ход переходит другой команде\n• подсказка удаляется из чата")),
            page(
                    b("Количество отгадок\n"),
                    t("• команда обязана выбрать минимум 1 объект\n• после первого ответа можно остановиться\n• попытки не ограничены до серого или чёрного объекта\n\n"),
                    b("Окончание игры\n"),
                    t("• все свои объекты раскрыты → "),
                    cb("победа\n", GOLD),
                    t("• открыт чёрный объект → "),
                    cb("поражение\n\n", DARK_RED),
                    b("Следующий раунд\n"),
                    t("Колода: "),
                    b("210 объектов"),
                    t(", хватает на "),
                    b("10 уникальных раундов.\n"),
                    t("Первый шанс — "),
                    b("50%"),
                    t(", после использования — "),
                    b("1%"),
                    t(" до кнопки «Сброс»."))
    };

    private static final String[] FULL_RULES = {
            page(
                    cb("Подробные правила\n", CYAN),
                    b("Таймер\n"),
                    t("В свой ход лидер пишет в специальный чат "),
                    b("одно слово-подсказку"),
                    t(" и число — сколько объектов команды связано с ней.\n"),
                    t("Команда обсуждает "),
                    b("1 минуту"),
                    t(" и выбирает объекты.\n\n"),
                    b("О подсказках\n"),
                    t("Лидер придумывает "),
                    b("одно ключевое слово"),
                    t(", связанное с несколькими объектами на стене.")),
            page(
                    b("О подсказках\n"),
                    t("После слова лидер обязательно называет число: сколько объектов относится к подсказке.\n\n"),
                    b("Пример\n"),
                    t("Если объекты — "),
                    b("КРИПЕР"),
                    t(" и "),
                    b("БЛОК"),
                    t(", можно сказать: "),
                    cb("Зелёный 2\n\n", BLUE),
                    t("Подсказка может быть и на один объект, но интереснее объединять "),
                    b("два или больше объектов"),
                    t(". Четыре одной подсказкой — большой успех.")),
            page(
                    b("Ограничения\n"),
                    t("Нельзя давать подсказки, которые:\n• указывают на "),
                    b("буквы в названии\n"),
                    t("• намекают на "),
                    b("расположение объектов"),
                    t(" на стене или столе\n\n"),
                    t("Буквы и цифры разрешены, если связаны "),
                    b("по смыслу"),
                    t(". Например, "),
                    b("Восемь 3"),
                    t(" для ШАР, НОМЕР и ОСЬМИНОГ.")),
            page(
                    b("Язык подсказок\n"),
                    t("Игра ведётся "),
                    b("на русском языке"),
                    t(". Иностранные слова можно использовать только если они широко вошли в русскую речь.\n\n"),
                    b("Пример\n"),
                    t("Нельзя "),
                    i("Apfel"),
                    t(" для ЯБЛОКО и БЕРЛИН, но можно "),
                    i("штрудель"),
                    t(".\n\n"),
                    b("Запрещено\n"),
                    t("• однокоренные слова\n• части сложных слов, пока объект не открыт")),
            page(
                    b("Сложные слова\n"),
                    t("Если на стене есть "),
                    b("СУПЕРГЕРОЙ"),
                    t(", пока он не закрыт, нельзя говорить «герой», «супер», «антигерой», «суперсила».\n\n"),
                    b("Омофоны и омонимы\n"),
                    t("Слова, которые звучат одинаково, но пишутся и означают разное, считаются "),
                    b("разными словами"),
                    t(". Поэтому РОТ не подходит для РОД.")),
            page(
                    b("Омонимы\n"),
                    t("Слова, которые пишутся одинаково, считаются одним словом. «Коса» может быть подсказкой для ТРАВА и ПРИНЦЕССА.\n\n"),
                    b("Сокращения\n"),
                    t("ЦРУ формально не одно слово, но такая подсказка допустима. Можно договориться про СПб, ЮНЕСКО, вуз, лазер и т.п.")),
            page(
                    b("Имена собственные\n"),
                    t("Имена собственные считаются корректными, если не нарушают остальные правила. Можно договориться считать многословные имена одним словом, например «Три мушкетёра» или Нью-Йорк.\n\n"),
                    t("Лидерам "),
                    b("запрещено выдумывать имена"),
                    t(". «Жонг Ли Ронг» не подходит для КИТАЙ и ШАР.")),
            page(
                    b("Рифмы\n"),
                    t("Рифмы разрешены только если связаны "),
                    b("по смыслу"),
                    t(", а не просто по звучанию. «Сошка» не подходит для КОШКИ.\n\n"),
                    t("Если используете рифму, "),
                    b("нельзя сообщать об этом"),
                    t(" — игроки должны догадаться сами.\n\n"),
                    b("Не будьте слишком строги\n"),
                    t("При споре можно тихо советоваться с другим лидером.")),
            page(
                    b("Продвинутая: «0»\n"),
                    t("Число 0 значит, что "),
                    b("ни один объект"),
                    t(" на стене не относится к слову. Команда всё равно должна выбрать хотя бы один объект, но может выбрать любое количество.\n\n"),
                    b("Продвинутая: «Неограниченно»\n"),
                    t("Можно сказать вместо числа, чтобы доразгадать старые подсказки. Минус — неизвестен лимит. Плюс — лимита попыток нет.")),
            page(
                    b("Одно слово\n"),
                    t("Подсказка "),
                    b("всегда состоит из одного слова"),
                    t(". Нельзя добавлять пояснения вроде «это притянуто за уши…».\n\n"),
                    t("Подсказка не может быть однокоренной к открытым на стене объектам. После закрытия объекта его можно использовать в будущих подсказках.")),
            page(
                    b("Процесс отгадывания\n"),
                    t("После подсказки команда обсуждает её значение. Лидер "),
                    b("не слышит обсуждение"),
                    t(", но другая команда слышит.\n\n"),
                    t("Окончательный выбор: игрок нажимает "),
                    b("ПКМ"),
                    t(" по объекту.\n\n"),
                    b("Результаты\n"),
                    t("• свой объект → цвет команды, можно продолжать\n• нейтральный → белый, ход соперникам")),
            page(
                    b("Результаты выбора\n"),
                    t("• объект другой команды → цвет соперников, ход им\n• чёрный объект → блок чёрный, игра сразу заканчивается, команда проигрывает\n\n"),
                    b("Совет\n"),
                    t("Перед подсказкой проверьте, что она никак не связана с чёрным объектом.\n\n"),
                    b("Количество отгадок\n"),
                    t("Нужно сделать хотя бы один выбор; после этого можно передать ход.")),
            page(
                    b("Некорректные подсказки\n"),
                    t("Предмет "),
                    b("«!»"),
                    t(" нужен для оспаривания. Чтобы спор сработал, хотя бы один игрок из каждой команды должен нажать предмет.\n\n"),
                    c("! Подсказка была оспорена командой <>\n", RED),
                    c("! Участник из команды соперников должен подтвердить оспаривание в течение 30 секунд\n\n", RED),
                    t("Если подсказка некорректна: ход переходит другой команде, подсказка удаляется из чата.")),
            page(
                    b("Последующие ходы\n"),
                    t("Лидеры по очереди дают подсказки. С каждым ходом объектов на поле меньше, отгадывать проще.\n\n"),
                    b("Окончание игры\n"),
                    t("• команда раскрыла все свои объекты → "),
                    cb("победа\n", GOLD),
                    t("• команда открыла чёрный объект → "),
                    cb("поражение\n\n", DARK_RED),
                    b("Подготовка\n"),
                    t("Колода: "),
                    b("210 объектов"),
                    t(", хватает на "),
                    b("10 уникальных раундов"),
                    t(". Первый шанс 50%, после использования 1% до кнопки «Сброс»."))
    };
}