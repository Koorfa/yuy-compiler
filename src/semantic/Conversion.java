package semantic;


public enum Conversion {
    NONE,           // Типы совпадают, преобразование не требуется
    INT_TO_REAL,    // integer -> real
    REAL_TO_INT,    // real -> integer
    BOOL_TO_INT,    // boolean -> integer (true->1, false->0)
    INT_TO_BOOL,    // integer -> boolean (1->true, 0->false, иначе ошибка)
    BOOL_TO_REAL    // boolean -> real (true->1.0, false->0.0)
}