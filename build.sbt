androidBuild

javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil

platformTarget in Android := "android-21"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

val createNoteImages = TaskKey[Unit]("create-note-images", "Creates the notes images. This is a task since a resource generator isn't executed in the right moment.")

createNoteImages := {
  import abc.parser.TuneParser
  import abc.ui.swing.JScoreComponent
  val root = (sourceManaged in Compile).value 
  val dir = root / "main" / "res" / "raw"
  dir.mkdirs()
  for(note <- "CDEFGABcdefgab") {
      val fileName = if (note.toLower == note) { note + "4" } else { note.toLower + "3" }
      val file = dir / ("imgnote" + fileName + ".png")
      val tuneAsString = "X:0\nK:D\n" + note
      val tune = new TuneParser().parse(tuneAsString)
      val scoreUI = new JScoreComponent()
      scoreUI.setTune(tune)
      scoreUI.writeScoreTo(file)
  }
}