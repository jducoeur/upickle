import mill._, mill.scalalib._, mill.scalalib.publish._, mill.scalajslib._

trait CommonModule extends ScalaModule {
  def scalacOptions = T{ if (scalaVersion() == "2.12.7") Seq("-opt:l:method") else Nil }
  def platformSegment: String

  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platformSegment"
  )
}
trait CommonPublishModule extends CommonModule with PublishModule with CrossScalaModule{
  def publishVersion = "0.7.1"
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/upickle",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/lihaoyi/upickle.git",
      "scm:git://github.com/lihaoyi/upickle.git"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )
}

trait OldCommonTestModule extends CommonModule with TestModule{
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.6.6", ivy"com.lihaoyi::acyclic:0.1.8")
  def testFrameworks = Seq("upickle.core.UTestFramework")
}
trait CommonTestModule extends CommonModule with TestModule{
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.6.9", ivy"com.lihaoyi::acyclic:0.2.0")
  def testFrameworks = Seq("upickle.core.UTestFramework")
}

trait OldCommonJvmModule extends CommonPublishModule{
  def platformSegment = "jvm"
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with OldCommonTestModule{
    def platformSegment = "jvm"
  }
}
trait CommonJvmModule extends CommonPublishModule{
  def platformSegment = "jvm"
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with CommonTestModule{
    def platformSegment = "jvm"
  }
}

trait OldCommonJsModule extends CommonPublishModule with ScalaJSModule{
  def platformSegment = "js"
  def scalaJSVersion = "0.6.25"
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with OldCommonTestModule{
    def platformSegment = "js"
    def scalaJSVersion = "0.6.25"
  }
}
trait CommonJsModule extends CommonPublishModule with ScalaJSModule{
  def platformSegment = "js"
  def scalaJSVersion = "0.6.28"
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with CommonTestModule{
    def platformSegment = "js"
    def scalaJSVersion = "0.6.28"
  }
}

object core extends Module {
  object jsOld extends Cross[OldCoreJsModule]("2.11.12")
  object js extends Cross[CoreJsModule]("2.12.7", "2.13.0")

  class OldCoreJsModule(val crossScalaVersion: String) extends OldCommonJsModule {
    def artifactName = "upickle-core"
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat::2.1.2"
    )

    object test extends Tests
  }
  class CoreJsModule(val crossScalaVersion: String) extends CommonJsModule {
    def artifactName = "upickle-core"
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat::2.1.2"
    )

    object test extends Tests
  }

  object jvmOld extends Cross[OldCoreJvmModule]("2.11.12")
  object jvm extends Cross[CoreJvmModule]("2.12.7", "2.13.0")
  class OldCoreJvmModule(val crossScalaVersion: String) extends OldCommonJvmModule {
    def artifactName = "upickle-core"
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat:2.1.2"
    )

    object test extends Tests
  }
  class CoreJvmModule(val crossScalaVersion: String) extends CommonJvmModule {
    def artifactName = "upickle-core"
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat:2.1.2"
    )

    object test extends Tests
  }
}


object implicits extends Module {

  trait OldImplicitsModule extends CommonPublishModule{
    def compileIvyDeps = Agg(
      ivy"com.lihaoyi::acyclic:0.1.8",
      ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
    )
    def generatedSources = T{
      val dir = T.ctx().dest
      val file = dir / "upickle" / "Generated.scala"
      ammonite.ops.mkdir(dir / "upickle")
      val tuples = (1 to 22).map{ i =>
        def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")
        val writerTypes = commaSeparated(j => s"T$j: Writer")
        val readerTypes = commaSeparated(j => s"T$j: Reader")
        val typeTuple = commaSeparated(j => s"T$j")
        val implicitWriterTuple = commaSeparated(j => s"implicitly[Writer[T$j]]")
        val implicitReaderTuple = commaSeparated(j => s"implicitly[Reader[T$j]]")
        val lookupTuple = commaSeparated(j => s"x(${j-1})")
        val fieldTuple = commaSeparated(j => s"x._$j")
        s"""
        implicit def Tuple${i}Writer[$writerTypes]: TupleNWriter[Tuple$i[$typeTuple]] =
          new TupleNWriter[Tuple$i[$typeTuple]](Array($implicitWriterTuple), x => if (x == null) null else Array($fieldTuple))
        implicit def Tuple${i}Reader[$readerTypes]: TupleNReader[Tuple$i[$typeTuple]] =
          new TupleNReader(Array($implicitReaderTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
        """
      }

      ammonite.ops.write(file, s"""
      package upickle.implicits
      import acyclic.file
      import language.experimental.macros
      /**
       * Auto-generated picklers and unpicklers, used for creating the 22
       * versions of tuple-picklers and case-class picklers
       */
      trait Generated extends upickle.core.Types{
        ${tuples.mkString("\n")}
      }
    """)
      Seq(PathRef(dir))
    }

  }
  trait ImplicitsModule extends CommonPublishModule{
    def compileIvyDeps = Agg(
      ivy"com.lihaoyi::acyclic:0.2.0",
      ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
    )
    def generatedSources = T{
      val dir = T.ctx().dest
      val file = dir / "upickle" / "Generated.scala"
      ammonite.ops.mkdir(dir / "upickle")
      val tuples = (1 to 22).map{ i =>
        def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")
        val writerTypes = commaSeparated(j => s"T$j: Writer")
        val readerTypes = commaSeparated(j => s"T$j: Reader")
        val typeTuple = commaSeparated(j => s"T$j")
        val implicitWriterTuple = commaSeparated(j => s"implicitly[Writer[T$j]]")
        val implicitReaderTuple = commaSeparated(j => s"implicitly[Reader[T$j]]")
        val lookupTuple = commaSeparated(j => s"x(${j-1})")
        val fieldTuple = commaSeparated(j => s"x._$j")
        s"""
        implicit def Tuple${i}Writer[$writerTypes]: TupleNWriter[Tuple$i[$typeTuple]] =
          new TupleNWriter[Tuple$i[$typeTuple]](Array($implicitWriterTuple), x => if (x == null) null else Array($fieldTuple))
        implicit def Tuple${i}Reader[$readerTypes]: TupleNReader[Tuple$i[$typeTuple]] =
          new TupleNReader(Array($implicitReaderTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
        """
      }

      ammonite.ops.write(file, s"""
      package upickle.implicits
      import acyclic.file
      import language.experimental.macros
      /**
       * Auto-generated picklers and unpicklers, used for creating the 22
       * versions of tuple-picklers and case-class picklers
       */
      trait Generated extends upickle.core.Types{
        ${tuples.mkString("\n")}
      }
    """)
      Seq(PathRef(dir))
    }

  }
  object jsOld extends Cross[OldJsModule]("2.11.12")
  object js extends Cross[JsModule]("2.12.7", "2.13.0")

  class OldJsModule(val crossScalaVersion: String) extends OldImplicitsModule with OldCommonJsModule{
    def moduleDeps = Seq(core.jsOld())
    def artifactName = "upickle-implicits"

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jsOld().test, core.jsOld().test)
    }
  }
  class JsModule(val crossScalaVersion: String) extends ImplicitsModule with CommonJsModule{
    def moduleDeps = Seq(core.js())
    def artifactName = "upickle-implicits"

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.js().test, core.js().test)
    }
  }

  object jvmOld extends Cross[OldJvmModule]("2.11.12")
  object jvm extends Cross[JvmModule]("2.12.7", "2.13.0")
  class OldJvmModule(val crossScalaVersion: String) extends OldImplicitsModule with OldCommonJvmModule{
    def moduleDeps = Seq(core.jvmOld())
    def artifactName = "upickle-implicits"
    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvmOld().test, core.jvmOld().test)
    }
  }
  class JvmModule(val crossScalaVersion: String) extends ImplicitsModule with CommonJvmModule{
    def moduleDeps = Seq(core.jvm())
    def artifactName = "upickle-implicits"
    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm().test, core.jvm().test)
    }
  }
}

object upack extends Module {

  object jsOld extends Cross[OldJsModule]("2.11.12")
  object js extends Cross[JsModule]("2.12.7", "2.13.0")

  class OldJsModule(val crossScalaVersion: String) extends OldCommonJsModule {
    def moduleDeps = Seq(core.jsOld())
    def artifactName = "upack"

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jsOld().test, core.jsOld().test)
    }
  }
  class JsModule(val crossScalaVersion: String) extends CommonJsModule {
    def moduleDeps = Seq(core.js())
    def artifactName = "upack"

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.js().test, core.js().test)
    }
  }

  object jvmOld extends Cross[OldJvmModule]("2.11.12")
  object jvm extends Cross[JvmModule]("2.12.7", "2.13.0")
  class OldJvmModule(val crossScalaVersion: String) extends OldCommonJvmModule {
    def moduleDeps = Seq(core.jvmOld())
    def artifactName = "upack"
    object test extends Tests with CommonModule  {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvmOld().test, core.jvmOld().test)
    }
  }
  class JvmModule(val crossScalaVersion: String) extends CommonJvmModule {
    def moduleDeps = Seq(core.jvm())
    def artifactName = "upack"
    object test extends Tests with CommonModule  {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm().test, core.jvm().test)
    }
  }
}


object ujson extends Module{
  trait OldJsonModule extends CommonPublishModule{
    def artifactName = "ujson"
    trait JawnTestModule extends OldCommonTestModule{
      def ivyDeps = T{
        if (scalaVersion() == "2.13.0") Agg()
        else Agg(
          ivy"org.scalatest::scalatest::3.0.7",
          ivy"org.scalacheck::scalacheck::1.14.0"
        )
      }
      def sources = T.sources(
        if (scalaVersion() == "2.13.0") Nil
        else super.sources()
      )

      def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
  }
  trait JsonModule extends CommonPublishModule{
    def artifactName = "ujson"
    trait JawnTestModule extends CommonTestModule{
      def ivyDeps = T{
        if (scalaVersion() == "2.13.0") Agg()
        else Agg(
          ivy"org.scalatest::scalatest::3.0.7",
          ivy"org.scalacheck::scalacheck::1.14.0"
        )
      }
      def sources = T.sources(
        if (scalaVersion() == "2.13.0") Nil
        else super.sources()
      )

      def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
  }

  object jsOld extends Cross[OldJsModule]("2.11.12")
  object js extends Cross[JsModule]("2.12.7", "2.13.0")
  class OldJsModule(val crossScalaVersion: String) extends OldJsonModule with OldCommonJsModule{
    def moduleDeps = Seq(core.jsOld())

    object test extends Tests with JawnTestModule
  }
  class JsModule(val crossScalaVersion: String) extends JsonModule with CommonJsModule{
    def moduleDeps = Seq(core.js())

    object test extends Tests with JawnTestModule
  }

  object jvmOld extends Cross[OldJvmModule]("2.11.12")
  object jvm extends Cross[JvmModule]("2.12.7", "2.13.0")
  class OldJvmModule(val crossScalaVersion: String) extends OldJsonModule with OldCommonJvmModule{
    def moduleDeps = Seq(core.jvmOld())
    object test extends Tests with JawnTestModule
  }
  class JvmModule(val crossScalaVersion: String) extends JsonModule with CommonJvmModule{
    def moduleDeps = Seq(core.jvm())
    object test extends Tests with JawnTestModule
  }

  object argonaut extends Cross[ArgonautModule]("2.12.7", "2.13.0")
  class ArgonautModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = "ujson-argonaut"
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(ivy"io.argonaut::argonaut:6.2.3")
  }
  object json4s extends Cross[Json4sModule]("2.12.7", "2.13.0")
  class Json4sModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = "ujson-json4s"
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(
      ivy"org.json4s::json4s-ast:3.6.7",
      ivy"org.json4s::json4s-native:3.6.7"
    )
  }

  object circe extends Cross[CirceModule]("2.12.7")
  class CirceModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = "ujson-circe"
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(ivy"io.circe::circe-parser:0.11.1")
  }

  object playOld extends Cross[OldPlayModule]("2.11.12")
  object play extends Cross[PlayModule]("2.12.7", "2.13.0")
  class OldPlayModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = "ujson-play"
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvmOld())
    def ivyDeps = Agg(
      ivy"com.typesafe.play::play-json:2.7.4",
      ivy"com.fasterxml.jackson.core:jackson-databind:2.9.4"
    )
  }
  class PlayModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = "ujson-play"
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(
      ivy"com.typesafe.play::play-json:2.7.4",
      ivy"com.fasterxml.jackson.core:jackson-databind:2.9.4"
    )
  }
}

trait UpickleModule extends CommonPublishModule{
  def artifactName = "upickle"
  def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"com.lihaoyi::acyclic:0.1.8"
  )
  def compileIvyDeps = Agg(
    ivy"com.lihaoyi::acyclic:0.1.8",
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}",
    ivy"org.scala-lang:scala-compiler:${scalaVersion()}"
  )
  def scalacOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8",
    "-feature"
  )
}


object upickle extends Module{
  object jvmOld extends Cross[OldJvmModule]("2.11.12")
  object jvm extends Cross[JvmModule]("2.12.7", "2.13.0")
  class OldJvmModule(val crossScalaVersion: String) extends UpickleModule with OldCommonJvmModule{
    def moduleDeps = Seq(ujson.jvmOld(), upack.jvmOld(), implicits.jvmOld())

    object test extends Tests with CommonModule{
      def moduleDeps = super.moduleDeps ++ Seq(
        ujson.playOld(),
        core.jvmOld().test
      )
      def ivyDeps = super.ivyDeps()// ++ bench.jvm.ivyDeps()
      def testFrameworks = Seq("upickle.core.UTestFramework")
    }
  }
  class JvmModule(val crossScalaVersion: String) extends UpickleModule with CommonJvmModule{
    def moduleDeps = Seq(ujson.jvm(), upack.jvm(), implicits.jvm())

    object test extends Tests with CommonModule{
      def moduleDeps = super.moduleDeps ++ Seq(
        ujson.argonaut(),
//        ujson.circe(),
        ujson.json4s(),
        ujson.play(),
        core.jvm().test
      )
      def ivyDeps = super.ivyDeps()// ++ bench.jvm.ivyDeps()
      def testFrameworks = Seq("upickle.core.UTestFramework")
    }
  }

  object jsOld extends Cross[OldJsModule]("2.11.12")
//  object js extends Cross[JsModule]("2.12.7", "2.13.0")

  class OldJsModule(val crossScalaVersion: String) extends UpickleModule with OldCommonJsModule {
    def moduleDeps = Seq(ujson.jsOld(), upack.jsOld(), implicits.jsOld())

    def scalacOptions = T{
      super.scalacOptions() ++ Seq({
        val a = build.millSourcePath.toString.replaceFirst("[^/]+/?$", "")
        val g = "https://raw.githubusercontent.com/lihaoyi/upickle"
        s"-P:scalajs:mapSourceURI:$a->$g/v${publishVersion()}/"
      })
    }
    object test extends Tests with CommonModule{
      def moduleDeps = super.moduleDeps ++ Seq(core.jsOld().test)
    }
  }
  class JsModule(val crossScalaVersion: String) extends UpickleModule with CommonJsModule {
    def moduleDeps = Seq(ujson.js(), upack.js(), implicits.js())

    def scalacOptions = T{
      super.scalacOptions() ++ Seq({
        val a = build.millSourcePath.toString.replaceFirst("[^/]+/?$", "")
        val g = "https://raw.githubusercontent.com/lihaoyi/upickle"
        s"-P:scalajs:mapSourceURI:$a->$g/v${publishVersion()}/"
      })
    }
    object test extends Tests with CommonModule{
      def moduleDeps = super.moduleDeps ++ Seq(core.js().test)
    }
  }
}
//
//trait BenchModule extends CommonModule{
//  def scalaVersion = "2.12.7"
//  def millSourcePath = build.millSourcePath / "bench"
//  def ivyDeps = Agg(
//    ivy"io.circe::circe-core::0.11.1",
//    ivy"io.circe::circe-generic::0.11.1",
//    ivy"io.circe::circe-parser::0.11.1",
//    ivy"com.typesafe.play::play-json::2.7.2",
//    ivy"io.argonaut::argonaut:6.2.3",
//    ivy"org.json4s::json4s-ast:3.6.5",
//    ivy"com.lihaoyi::sourcecode::0.1.5",
//  )
//}
// Bench is causing compile problems for some reason. We don't care passionately, so let's take it out for now:
//
//object bench extends Module {
//  object js extends BenchModule with ScalaJSModule {
//    def scalaJSVersion = "0.6.25"
//    def platformSegment = "js"
//    def moduleDeps = Seq(upickle.js("2.12.7").test)
//    def run(args: String*) = T.command {
//      finalMainClassOpt() match{
//        case Left(err) => mill.eval.Result.Failure(err)
//        case Right(_) =>
//          ScalaJSWorkerApi.scalaJSWorker().run(
//            toolsClasspath().map(_.path),
//            nodeJSConfig(),
//            fullOpt().path.toIO
//          )
//          mill.eval.Result.Success(())
//      }
//    }
//  }
//
//  object jvm extends BenchModule {
//    def platformSegment = "jvm"
//    def moduleDeps = Seq(upickle.jvm("2.12.7").test)
//    def ivyDeps = super.ivyDeps() ++ Agg(
//      ivy"com.fasterxml.jackson.module::jackson-module-scala:2.9.8",
//      ivy"com.fasterxml.jackson.core:jackson-databind:2.9.4",
//    )
//  }
//}
