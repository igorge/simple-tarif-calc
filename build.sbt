import android.Keys._

android.Plugin.androidBuild

name := "simple_tarif_calc"

version := "0.1"

scalaVersion := "2.10.4"

proguardCache in Android ++= Seq(
  ProguardCache("org.scaloid") % "org.scaloid"
)

proguardOptions in Android ++= Seq(
  //"-verbose",
  "-dontobfuscate",
  "-dontoptimize",
  //"-optimizationpasses 6",
  "-dontwarn net.pocorall.**", "-dontwarn com.google.**",
  "-keep public class * extends android.app.Activity",
  "-dontwarn net.miginfocom.**",
  "-dontwarn resource.jta.**"
//  "-keep class scala.collection.SeqLike",
  //"-keep class org.scaloid.**"
)

//resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq("org.scaloid" %% "scaloid" % "3.2-8")

libraryDependencies += "com.netflix.rxjava" % "rxjava-scala" % "0.17.2"

libraryDependencies += "com.miglayout" % "miglayout-core" % "4.2"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"

scalacOptions in Compile += "-feature"

//scalacOptions in Compile += "-optimize"

run <<= run in Android

install <<= install in Android


