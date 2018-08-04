package dummy_models

case class A(id: String)
case class B(a: A)
//case class D(a: A, b: List[B])
case class E(a: A, b: B, bs: List[B])

