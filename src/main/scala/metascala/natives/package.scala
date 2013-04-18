package metascala

/**
 * `natives` contains the bindings between method calls internal to the Metascala
 * VM and external functionality of the host JVM. It defines bindings in a
 * hierarchical fashion, as well as a default set of bindings in Default.Scala.
 *
 * A Metascala VM takes a set of Bindings on instantiation, which can be customized
 * e.g. to trapped the calls to native methods and redirect them to wherever you want.
 *
 */
package object natives {
  def value(x: => Val) = (vm: rt.Thread) => x
  def value1(x: => Val) = (vm: rt.Thread) => (a: Val) => x
  def value2(x: => Val) = (vm: rt.Thread) => (a: Val, b: Val) => x
  val noOp = value(0)
  val noOp1 = value1(0)
  val noOp2 = value2(0)

  /**
   * Implements a nice DSL to build the list of trapped method calls
   */
  implicit class pimpedRoute(val m: Seq[(String, Any)]) extends AnyVal{
    def toRoute(parts: List[String] = Nil): Seq[rt.Method.Native1] = {
      m.flatMap{ case (k, v) =>
        v match{
          case thing: Seq[(String, Any)] =>
            thing.toRoute(k :: parts).map {
              case rt.Method.Native1(clsName, sig, func) => rt.Method.Native1(if (clsName == "") k else k + "/" + clsName, sig, func)
            }

          case func: (`rt`.Thread => Any) =>
            val (name, descString) = k.splitAt(k.indexOf('('))
            val p = parts.reverse

            val fullDescString =
              descString
                .replaceAllLiterally("L//", s"L${p(0)}/${p(1)}/")
                .replaceAllLiterally("L/", s"L${p(0)}/")

            val desc = imm.Desc.read(fullDescString)

            type V = Val
            val newFunc = (vt: rt.Thread) => (args: Seq[Val]) => func(vt) match{
              case f: ((V, V, V, V, V) => V) => f(args(0), args(1), args(2), args(3), args(4))
              case f: ((V, V, V, V) => V) => f(args(0), args(1), args(2), args(3))
              case f: ((V, V, V) => V) => f(args(0), args(1), args(2))
              case f: ((V, V) => V) => f(args(0), args(1))
              case f: (V => V) => f(args(0))
              case f: V => f
            }
            Vector(rt.Method.Native1("", imm.Sig(name, desc), newFunc))
        }
      }
    }
  }
  implicit class pimpedMap(val s: String) extends AnyVal{
    def /(a: (String, Any)*) = s -> a
    def x(a: rt.Thread => Val) = s -> a
    def x1(a: rt.Thread => Nothing => Val) = s -> a
    def x2(a: rt.Thread => (Nothing, Nothing) => Val) = s -> a
    def x3(a: rt.Thread => (Nothing, Nothing, Nothing) => Val) = s -> a
    def x4(a: rt.Thread => (Nothing, Nothing, Nothing, Nothing) => Val) = s -> a
    def x5(a: rt.Thread => (Nothing, Nothing, Nothing, Nothing, Nothing) => Val) = s -> a
    def x6(a: rt.Thread => (Nothing, Nothing, Nothing, Nothing, Nothing, Nothing) => Val) = s -> a
  }
}
