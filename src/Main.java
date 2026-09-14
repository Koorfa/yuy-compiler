import java.util.List;

public class Main {
    public static void main(String[] args) {

        String test1 = """
                var x : integer is 15
                var y : integer is 4
                var w : real is 2.7
                var z : real is 3.0

                routine test_arithmetics()
                is
                    var i : integer
                    var r : real
                    i := x + y - 5
                    print i
                    i := x * y
                    print i
                    i := x / y
                    print i
                    i := x % y
                    print i
                    r := w + z
                    print r
                    r := z - w
                    print r
                    r := z * w
                    print r
                    r := z / w
                    print r
                    i := x + w
                    print i
                    i := x * w
                    print i
                    i := y / w
                    print i
                    i := y - w
                    print i
                    r := x + w
                    print r
                    r := x * w
                    print r
                    r := y / w
                    print r
                    r := y - w
                    print r
                end
                """;

        String test2 = """
                var a : boolean is true
                var d : boolean is true
                var b : boolean is false
                var e : boolean is false

                routine test_logics() is
                    var c : boolean
                    c := b and e
                    print c
                    c := b and a
                    print c
                    c := d and e
                    print c
                    c := a and d
                    print c
                    c := b or e
                    print c
                    c := b or a
                    print c
                    c := d or e
                    print c
                    c := a or d
                    print c
                    c := b xor e
                    print c
                    c := b xor a
                    print c
                    c := d xor e
                    print c
                    c := a xor d
                    print c
                    c := not a
                    print c
                    c := not b
                    print c
                    c := (a and b) or (not d)
                    print c
                    c := a and (b or d)
                    print c
                end
                """;

        String test3 = """
                var x : integer is 7
                var y : integer is 5

                routine test_conditional()
                is
                    if x > 3 then
                        print true
                    else
                        print false
                    end
                    if x > 10 then
                        print true
                    else
                        print false
                    end

                    if x = 7 then
                        print true
                    end
                    if x > 4 then
                        if y /= 5 then
                            print true
                        else
                            print false
                        end
                    else
                        if y >= 3 then
                            print false
                        else
                            print true
                        end
                    end
                end
                """;

        String test4 = """
                var i : integer is 1
                var sum : integer is 0

                routine test_while() is
                    while i <= 10 loop
                        if i % 2 = 0 then
                            sum := sum + i
                        end
                        i := i + 1
                    end
                    print sum
                end
                """;

        String test4ZeroIterations = """
                var i : integer is 10
                var sum : integer is 0

                routine test_while_zero_iterations()
                is
                    while i <= 5 loop
                        sum := sum + i
                        i := i + 1
                    end
                    print sum
                end
                """;

        String test5 = """
                var a : integer is 5
                var b : real is 2.5

                routine main() is
                    var x : real
                    var y : integer

                    x := a
                    y := b

                    print x
                    print y
                    print true + 1.5 + 2
                end
                """;

        String test6 = """
                var sum : integer is 0

                routine test_for() is
                    for i in 1 .. 5
                    loop
                        sum := sum + i
                    end

                    print sum
                end
                """;

        String test7 = """
                routine test_reverse_for() is
                    var result : integer is 0

                    for i in 5 .. 1 reverse loop
                        result := result * 10 + i
                    end

                    print result
                end
                """;

        String test8 = """
                type Numbers is array [5]
                integer

                routine test_array() is
                    var a : Numbers

                    a[1] := 10
                    a[2] := 20
                    a[3] := 30
                    a[4] := 40
                    a[5] := 50

                    print a[1]
                    print a[3]
                    print a[5]
                end
                """;

        String test9 = """
                routine main() is
                    var matrix : array [2] array [3] integer

                    matrix[1][1] := 1
                    matrix[1][2] := 2
                    matrix[1][3] := 3

                    matrix[2][1] := 4
                    matrix[2][2] := 5
                    matrix[2][3] := 6

                    print matrix[2][1]
                    print matrix[2][2]
                    print matrix[2][3]
                    print matrix[1][1]
                    print matrix[1][2]
                    print matrix[1][3]
                end
                """;

        String test10 = """
                routine double_array(arr : array [3] integer) : array [3] integer is
                    var result : array [3] integer
                    result[1] := arr[1] * 2
                    result[2] := arr[2] * 2
                    result[3] := arr[3] * 2
                    => result
                end

                routine main() is
                    var original : array [3] integer
                    original[1] := 1
                    original[2] := 2
                    original[3] := 3

                    var doubled : array [3] integer
                    doubled := double_array(original)

                    print doubled[1]
                    print doubled[2]
                    print doubled[3]
                end
                """;

        String test11 = """
                routine add(x : integer, y : integer) :
                integer
                => x + y

                routine test_routine() is
                    var result : integer

                    result := add(3, 7)

                    print result
                end
                """;

        String test12 = """
                routine test_for_break() is
                    var sum : integer is 0
                    for i in 1 .. 10 loop
                        if i = 5 then
                            break
                        end
                        sum := sum + i
                    end
                    print sum
                end

                routine test_for_continue() is
                    var sum : integer is 0
                    for i in 1 .. 10 loop
                        if i % 2 = 0 then
                            continue
                        end
                        sum := sum + i
                    end
                    print sum
                end

                routine test_while_break() is
                    var i : integer is 1
                    var sum : integer is 0
                    while i <= 10 loop
                        if i = 5 then
                            break
                        end
                        sum := sum + i
                        i := i + 1
                    end
                    print sum
                end

                routine test_while_continue()
                is
                    var i : integer is 1
                    var sum : integer is 0
                    while i <= 10 loop
                        if i % 2 = 0 then
                            i := i + 1
                            continue
                        end
                        sum := sum + i
                        i := i + 1
                    end
                    print sum
                end
                """;

        String test13 = """
                var x: integer is 100

                routine demonstrate() is
                    print x

                    var x: integer is 200
                    print x

                    if true then
                        var x: integer is 300
                        print x
                    end

                    print x
                end

                print x
                demonstrate()
                print x
                """;

        String test14 = """
                routine test_precedence() is
                    var pass: integer is 0
                    var fail: integer is 0

                    if 2 + 3 * 4 = 14 then pass := pass + 1 else fail := fail + 1 end
                    if 24 / 4 / 2 = 3 then pass := pass + 1 else fail := fail + 1 end
                    if 10 - 4 - 2 = 4 then pass := pass + 1 else fail := fail + 1 end
                    if -2 * 3 = -6 then pass := pass + 1 else fail := fail + 1 end
                    if (2 + 3) * 4 = 20 then pass := pass + 1 else fail := fail + 1 end
                    if 10 - 2 * 3 % 4 + 1 = 9 then pass := pass + 1 else fail := fail + 1 end
                    if 2 + 2 = 4 and 3 * 3 > 8 then pass := pass + 1 else fail := fail + 1 end
                    if 5 - 2 /= 4 then pass := pass + 1 else fail := fail + 1 end

                    print pass
                    print fail
                end
                """;

        String test15 = """
                type Pair is record
                    var x: integer
                    var y: integer
                end

                routine start() is
                    var p: Pair
                    p.x := 10
                    p.y := 20
                    print p.x, p.y
                end
                """;

        String test16 = """
                type Date is record
                    var day : integer
                    var month : integer
                    var year : integer
                end

                routine print_date(d : Date) is
                    print d.day, d.month, d.year
                end

                type Person is record
                    var birthday: Date
                    var id: integer
                end

                routine print_person(p : Person) is
                    print p.id
                    print_date(p.birthday)
                end

                routine main() is
                    var bd : Date
                    bd.day := 1
                    bd.month := 1
                    bd.year := 2000
                    var person : Person
                    person.birthday := bd
                    person.id := 1
                    print_person(person)
                end
                """;

        String test17 = """
                type Point is record
                    var x: integer
                    var y: integer
                end

                routine create_point(px : integer, py : integer) : Point is
                    var p : Point
                    p.x := px
                    p.y := py
                    => p
                end

                routine move_point_by(p: Point, dx : integer, dy : integer): Point is
                    p.x := p.x + dx
                    p.y := p.y + dy
                    => p
                end
                """;

        System.out.println("Test 1");
        runTest(test1);

        System.out.println("Test 2");
        runTest(test2);

        System.out.println("Test 3");
        runTest(test3);

        System.out.println("Test 4");
        runTest(test4);

        System.out.println("Test 4 zero iterations");
        runTest(test4ZeroIterations);

        System.out.println("Test 5");
        runTest(test5);

        System.out.println("Test 6");
        runTest(test6);

        System.out.println("Test 7");
        runTest(test7);

        System.out.println("Test 8");
        runTest(test8);

        System.out.println("Test 9");
        runTest(test9);

        System.out.println("Test 10");
        runTest(test10);

        System.out.println("Test 11");
        runTest(test11);

        System.out.println("Test 12");
        runTest(test12);

        System.out.println("Test 13");
        runTest(test13);

        System.out.println("Test 14");
        runTest(test14);

        System.out.println("Test 15");
        runTest(test15);

        System.out.println("Test 16");
        runTest(test16);

        System.out.println("Test 17");
        runTest(test17);
    }

    private static void runTest(String source) {
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (LexerException e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
    }
}