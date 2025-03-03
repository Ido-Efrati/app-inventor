// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.appinventor.buildserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import com.android.sdklib.build.ApkBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Main entry point for the YAIL compiler.
 *
 * <p>Supplies entry points for building Young Android projects.
 *
 * @author markf@google.com (Mark Friedman)
 * @author lizlooney@google.com (Liz Looney)
 */
public final class Compiler {
  // Kawa and DX processes can use a lot of memory. We only launch one Kawa or DX process at a time.
  private static final Object SYNC_KAWA_OR_DX = new Object();

  // TODO(sharon): temporary until we add support for new activities
  private static final String LIST_ACTIVITY_CLASS =
      "com.google.appinventor.components.runtime.ListPickerActivity";

  private static final String WEBVIEW_ACTIVITY_CLASS =
      "com.google.appinventor.components.runtime.WebViewActivity";

  public static final String RUNTIME_FILES_DIR = "/files/";

  private static final String DEFAULT_ICON =
      RUNTIME_FILES_DIR + "ya.png";
  
  private static final String DEFAULT_VERSION_CODE = "1";
  private static final String DEFAULT_VERSION_NAME = "1.0";

  private static final String COMPONENT_PERMISSIONS =
      RUNTIME_FILES_DIR + "simple_components_permissions.json";

  /*
   * Resource paths to yail runtime, runtime library files and sdk tools.
   * To get the real file paths, call getResource() with one of these constants.
   */
  private static final String SIMPLE_ANDROID_RUNTIME_JAR =
      RUNTIME_FILES_DIR + "AndroidRuntime.jar";
  private static final String ANDROID_RUNTIME =
      RUNTIME_FILES_DIR + "android.jar";
  private static final String MAC_AAPT_TOOL =
      "/tools/mac/aapt";
  private static final String WINDOWS_AAPT_TOOL = 
      "/tools/windows/aapt";
  private static final String LINUX_AAPT_TOOL =
      "/tools/linux/aapt";
  private static final String KAWA_RUNTIME =
      RUNTIME_FILES_DIR + "kawa.jar";
  private static final String TWITTER_RUNTIME =
      RUNTIME_FILES_DIR + "twitter4j.jar";
  private static final String DX_JAR =
      RUNTIME_FILES_DIR + "dx.jar";
  @VisibleForTesting
  static final String YAIL_RUNTIME =
      RUNTIME_FILES_DIR + "runtime.scm";

  // Logging support
  private static final Logger LOG = Logger.getLogger(Compiler.class.getName());

  private static final ConcurrentMap<String, Set<String>> componentPermissions =
      new ConcurrentHashMap<String, Set<String>>();

  /**
   * Map used to hold the names and paths of resources that we've written out
   * as temp files.
   * Don't use this map directly. Please call getResource() with one of the
   * constants above to get the (temp file) path to a resource.
   */
  private static final ConcurrentMap<String, File> resources =
      new ConcurrentHashMap<String, File>();

  // TODO(user,lizlooney): i18n here and in lines below that call String.format(...)
  private static final String ERROR_IN_STAGE =
      "Error: Your build failed due to an error in the %s stage, " +
      "not because of an error in your program.\n";
  private static final String ICON_ERROR =
      "Error: Your build failed because %s cannot be used as the application icon.\n";
  private static final String NO_USER_CODE_ERROR =
      "Error: No user code exists.\n";
  private static final String COMPILATION_ERROR =
      "Error: Your build failed due to an error when compiling %s.\n";

  private final Project project;
  private final Set<String> componentTypes;
  private final PrintStream out;
  private final PrintStream err;
  private final PrintStream userErrors;
  private final boolean isForRepl;
  // Maximum ram that can be used by a child processes, in MB.
  private final int childProcessRamMb;


  /*
   * Generate the set of Android permissions needed by this project.
   */
  @VisibleForTesting
  Set<String> generatePermissions() {
    // Before we can use componentPermissions, we have to call loadComponentPermissions().
    try {
      loadComponentPermissions();
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
      return null;
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
      return null;
    }

    Set<String> permissions = Sets.newHashSet();
    for (String componentType : componentTypes) {
      permissions.addAll(componentPermissions.get(componentType));
    }
    return permissions;
  }

  /*
   * Creates an AndroidManifest.xml file needed for the Android application.
   */
  private boolean writeAndroidManifest(File manifestFile, Set<String> permissionsNeeded) {
    // Create AndroidManifest.xml
    String mainClass = project.getMainClass();
    String packageName = Signatures.getPackageName(mainClass);
    String className = Signatures.getClassName(mainClass);
    String projectName = project.getProjectName();
    String vCode = (project.getVCode() == null) ? DEFAULT_VERSION_CODE : project.getVCode();
    String vName = (project.getVName() == null) ? DEFAULT_VERSION_NAME : project.getVName();
    LOG.log(Level.INFO, "VCode: " + project.getVCode());
    LOG.log(Level.INFO, "VName: " + project.getVName());
    
    // TODO(user): Use com.google.common.xml.XmlWriter
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(manifestFile));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      // TODO(markf) Allow users to set versionCode and versionName attributes.
      // See http://developer.android.com/guide/publishing/publishing.html for
      // more info.
      out.write("<manifest " +
          "xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
          "package=\"" + packageName + "\" " +
          // TODO(markf): uncomment the following line when we're ready to enable publishing to the
          // Android Market.
         "android:versionCode=\"" + vCode +"\" " + "android:versionName=\"" + vName + "\" " +
          ">\n");
      for (String permission : permissionsNeeded) {
        out.write("  <uses-permission android:name=\"" + permission + "\" />\n");
      }
      // TODO(markf): Change the minSdkVersion below if we ever require an SDK beyond 1.5.
      // The market will use the following to filter apps shown to devices that don't support
      // the specified SDK version.  We might also want to allow users to specify minSdkVersion
      // or have us specify higher SDK versions when the program uses a component that uses
      // features from a later SDK (e.g. Bluetooth).
      out.write("  <uses-sdk android:minSdkVersion=\"3\" />\n");

      // If we set the targetSdkVersion to 4, we can run full size apps on tablets.
      // On non-tablet hi-res devices like a Nexus One, the screen dimensions will be the actual
      // device resolution. Unfortunately, images, canvas, sprites, and buttons with images are not
      // sized appropriately. For example, an image component with an image that is 60x60, width
      // and height properties set to automatic, is sized as 40x40. So they appear on the screen
      // much smaller than they should be. There is code in Canvas and ImageSprite to work around
      // this problem, but images and buttons are still an unsolved problem. We'll have to solve
      // that before we can set the targetSdkVersion to 4 here.
      // out.write("  <uses-sdk android:targetSdkVersion=\"4\" />\n");

      out.write("  <application ");

      // TODO(markf): The preparing to publish doc at
      // http://developer.android.com/guide/publishing/preparing.html suggests removing the
      // 'debuggable=true' but I'm not sure that our users would want that while they're still
      // testing their packaged apps.  Maybe we should make that an option, somehow.
      out.write("android:debuggable=\"true\" ");
      out.write("android:label=\"" + projectName + "\" ");
      out.write("android:icon=\"@drawable/ya\" ");
      out.write(">\n");

      for (Project.SourceDescriptor source : project.getSources()) {
        String formClassName = source.getQualifiedName();
        //String screenName = formClassName.substring(formClassName.lastIndexOf('.') + 1);
        boolean isMain = formClassName.equals(mainClass);

        if (isMain) {
          // The main activity of the application.
          out.write("    <activity android:name=\"." + className + "\" ");
        } else {
          // A secondary activity of the application.
          out.write("    <activity android:name=\"" + formClassName + "\" ");
        }
        out.write("android:windowSoftInputMode=\"stateHidden\" ");
        out.write("android:configChanges=\"orientation|keyboardHidden\">\n");

        out.write("      <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
        if (isMain && !isForRepl) {
          // We only want the LAUNCHER category if this is a normal user-compiled app.
          // If this is the special REPL app then we don't want the app to show up in
          // the apps list
          out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n");
        }
        out.write("      </intent-filter>\n");
        out.write("    </activity>\n");
      }

      // ListPickerActivity
      out.write("    <activity android:name=\"" + LIST_ACTIVITY_CLASS + "\" " +
          "android:configChanges=\"orientation|keyboardHidden\" " +
          "android:screenOrientation=\"behind\">\n");
      out.write("    </activity>\n");
      // WebViewActivity
      out.write("    <activity android:name=\"" + WEBVIEW_ACTIVITY_CLASS + "\" " +
          "android:configChanges=\"orientation|keyboardHidden\" " +
          "android:screenOrientation=\"behind\">\n");
      out.write("      <intent-filter>\n");
      out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
      out.write("      </intent-filter>\n");
      out.write("    </activity>\n");

      out.write("  </application>\n");
      out.write("</manifest>\n");
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "manifest"));
      return false;
    }

    return true;
  }

  /**
   * Builds a YAIL project.
   *
   * @param project  project to build
   * @param componentTypes component types used in the project
   * @param out  stdout stream for compiler messages
   * @param err  stderr stream for compiler messages
   * @param userErrors stream to write user-visible error messages
   * @param isForRepl {@code true}, if this compilation is for the special REPL app
   * @param keystoreFilePath
   * @param childProcessRam   maximum RAM for child processes, in MBs.
   * @return  {@code true} if the compilation succeeds, {@code false} otherwise
   */
  public static boolean compile(Project project, Set<String> componentTypes,
                                PrintStream out, PrintStream err, PrintStream userErrors,
                                boolean isForRepl, String keystoreFilePath, int childProcessRam) {
    long start = System.currentTimeMillis();

    // Create a new compiler instance for the compilation
    Compiler compiler = new Compiler(project, componentTypes, out, err, userErrors, isForRepl,
                                     childProcessRam);

    // Create build directory.
    File buildDir = createDirectory(project.getBuildDirectory());

    // Prepare application icon.
    out.println("________Preparing application icon");
    File resDir = createDirectory(buildDir, "res");
    File drawableDir = createDirectory(resDir, "drawable");
    if (!compiler.prepareApplicationIcon(new File(drawableDir, "ya.png"))) {
      return false;
    }

    // Determine android permissions.
    out.println("________Determining permissions");
    Set<String> permissionsNeeded = compiler.generatePermissions();
    if (permissionsNeeded == null) {
      return false;
    }

    // Generate AndroidManifest.xml
    out.println("________Generating manifest file");
    File manifestFile = new File(buildDir, "AndroidManifest.xml");
    if (!compiler.writeAndroidManifest(manifestFile, permissionsNeeded)) {
      return false;
    }

    // Create class files.
    out.println("________Compiling source files");
    File classesDir = createDirectory(buildDir, "classes");
    if (!compiler.generateClasses(classesDir)) {
      return false;
    }

    // Invoke dx on class files
    out.println("________Invoking DX");
    // TODO(markf): Running DX is now pretty slow (~25 sec overhead the first time and ~15 sec
    // overhead for subsequent runs).  I think it's because of the need to dx the entire
    // kawa runtime every time.  We should probably only do that once and then copy all the
    // kawa runtime dx files into the generated classes.dex (which would only contain the
    // files compiled for this project).
    // Aargh.  It turns out that there's no way to manipulate .dex files to do the above.  An
    // Android guy suggested an alternate approach of shipping the kawa runtime .dex file as
    // data with the application and then creating a new DexClassLoader using that .dex file
    // and with the original app class loader as the parent of the new one.
    File tmpDir = createDirectory(buildDir, "tmp");
    String dexedClasses = tmpDir.getAbsolutePath() + File.separator + "classes.dex";
    if (!compiler.runDx(classesDir, dexedClasses)) {
      return false;
    }

    // Invoke aapt to package everything up
    out.println("________Invoking AAPT");
    File deployDir = createDirectory(buildDir, "deploy");
    String tmpPackageName = deployDir.getAbsolutePath() + File.separatorChar +
        project.getProjectName() + ".ap_";
    if (!compiler.runAaptPackage(manifestFile, resDir, tmpPackageName)) {
      return false;
    }

    // Seal the apk with ApkBuilder
    out.println("________Invoking ApkBuilder");
    String apkAbsolutePath = deployDir.getAbsolutePath() + File.separatorChar +
        project.getProjectName() + ".apk";
    if (!compiler.runApkBuilder(apkAbsolutePath, tmpPackageName, dexedClasses)) {
      return false;
    }

    // Sign the apk file
    out.println("________Signing the apk file");
    if (!compiler.runJarSigner(apkAbsolutePath, keystoreFilePath)) {
      return false;
    }

    out.println("Build finished in " +
        ((System.currentTimeMillis() - start) / 1000.0) + " seconds");

    return true;
  }

  /*
   * Runs ApkBuilder by using the API instead of calling its main method because the main method
   * can call System.exit(1), which will bring down our server.
   */
  private boolean runApkBuilder(String apkAbsolutePath, String zipArchive, String dexedClasses) {
    try {
      ApkBuilder apkBuilder =
          new ApkBuilder(apkAbsolutePath, zipArchive, dexedClasses, null, System.out);
      apkBuilder.sealApk();
      return true;
    } catch (Exception e) {
      // This is fatal.
      e.printStackTrace();
      LOG.warning("YAIL compiler - ApkBuilder failed.");
      err.println("YAIL compiler - ApkBuilder failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "ApkBuilder"));
      return false;
    }
  }

  /**
   * Creates a new YAIL compiler.
   *
   * @param project  project to build
   * @param componentTypes component types used in the project
   * @param out  stdout stream for compiler messages
   * @param err  stderr stream for compiler messages
   * @param userErrors stream to write user-visible error messages
   * @param isForRepl {@code true}, if this compilation is for the special REPL app
   * @param childProcessMaxRam  maximum RAM for child processes, in MBs.
   */
  @VisibleForTesting
  Compiler(Project project, Set<String> componentTypes, PrintStream out, PrintStream err,
           PrintStream userErrors, boolean isForRepl, int childProcessMaxRam) {
    this.project = project;
    this.componentTypes = componentTypes;
    this.out = out;
    this.err = err;
    this.userErrors = userErrors;
    this.isForRepl = isForRepl;
    this.childProcessRamMb = childProcessMaxRam;
  }

  /*
   * Runs the Kawa compiler in a separate process to generate classes. Returns false if not able to
   * create a class file for every source file in the project.
   */
  private boolean generateClasses(File classesDir) {
    try {
      List<Project.SourceDescriptor> sources = project.getSources();
      List<String> sourceFileNames = Lists.newArrayListWithCapacity(sources.size());
      List<String> classFileNames = Lists.newArrayListWithCapacity(sources.size());
      boolean userCodeExists = false;
      for (Project.SourceDescriptor source : sources) {
        String sourceFileName = source.getFile().getAbsolutePath();
        LOG.log(Level.INFO, "source file: " + sourceFileName);
        int srcIndex = sourceFileName.indexOf("/../src/");
        String sourceFileRelativePath = sourceFileName.substring(srcIndex + 8);
        String classFileName = (classesDir.getAbsolutePath() + "/" + sourceFileRelativePath)
            .replace(YoungAndroidConstants.YAIL_EXTENSION, ".class");
        if (System.getProperty("os.name").startsWith("Windows")){
        	classFileName = classesDir.getAbsolutePath()
           .replace(YoungAndroidConstants.YAIL_EXTENSION, ".class");
        }

        // Check whether user code exists by seeing if a left parenthesis exists at the beginning of
        // a line in the file
        // TODO(user): Replace with more robust test of empty source file.
        if (!userCodeExists) {
          Reader fileReader = new FileReader(sourceFileName);
          try {
            while (fileReader.ready()) {
              int c = fileReader.read();
              if (c == '(') {
                userCodeExists = true;
                break;
              }
            }
          } finally {
            fileReader.close();
          }
        }
        sourceFileNames.add(sourceFileName);
        classFileNames.add(classFileName);
      }

      if (!userCodeExists) {
        userErrors.print(NO_USER_CODE_ERROR);
        return false;
      }

      String classpath =
          getResource(KAWA_RUNTIME) + File.pathSeparator +
          getResource(SIMPLE_ANDROID_RUNTIME_JAR) + File.pathSeparator +
          getResource(TWITTER_RUNTIME) + File.pathSeparator +
          getResource(ANDROID_RUNTIME);
      String yailRuntime = getResource(YAIL_RUNTIME);
      List<String> kawaCommandArgs = Lists.newArrayList();
      int mx = childProcessRamMb - 200;
      Collections.addAll(kawaCommandArgs,
          System.getProperty("java.home") + "/bin/java",
          "-mx" + mx + "M",
          "-cp", classpath,
          "kawa.repl",
          "-f", yailRuntime,
          "-d", classesDir.getAbsolutePath(),
          "-P", Signatures.getPackageName(project.getMainClass()) + ".",
          "-C");
      // TODO(lizlooney) - we are currently using (and have always used) absolute paths for the
      // source file names. The resulting .class files contain references to the source file names,
      // including the name of the tmp directory that contains them. We may be able to avoid that
      // by using source file names that are relative to the project root and using the project
      // root as the working directory for the Kawa compiler process.
      kawaCommandArgs.addAll(sourceFileNames);
      kawaCommandArgs.add(yailRuntime);
      String[] kawaCommandLine = kawaCommandArgs.toArray(new String[kawaCommandArgs.size()]);

      long start = System.currentTimeMillis();
      // Capture Kawa compiler stderr. The ODE server parses out the warnings and errors and adds
      // them to the protocol buffer for logging purposes. (See
      // YoungAndroidProjectBuilder.processCompilerOutout.
      ByteArrayOutputStream kawaOutputStream = new ByteArrayOutputStream();
      boolean kawaSuccess;
      synchronized (SYNC_KAWA_OR_DX) {
        kawaSuccess = Execution.execute(null, kawaCommandLine,
            System.out, new PrintStream(kawaOutputStream));
      }
      String kawaOutput = kawaOutputStream.toString();
      out.print(kawaOutput);
      String kawaCompileTimeMessage = "Kawa compile time: " +
          ((System.currentTimeMillis() - start) / 1000.0) + " seconds";
      out.println(kawaCompileTimeMessage);
      LOG.info(kawaCompileTimeMessage);

      // Check that all of the class files were created.
      // If they weren't, return with an error.
      for (String classFileName : classFileNames) {
        File classFile = new File(classFileName);
        if (!classFile.exists()) {
          LOG.log(Level.INFO, "Can't find class file: " + classFileName);
          String screenName = classFileName.substring(classFileName.lastIndexOf('/') + 1,
              classFileName.lastIndexOf('.'));
          userErrors.print(String.format(COMPILATION_ERROR, screenName));
          return false;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "compile"));
      return false;
    }

    return true;
  }

  private boolean runJarSigner(String apkAbsolutePath, String keystoreAbsolutePath) {
    // TODO(user): maybe make a command line flag for the jarsigner location
    String javaHome = System.getProperty("java.home");
    // This works on Mac OS X.
    File jarsignerFile = new File(javaHome + File.separator + "bin" +
        File.separator + "jarsigner");
    if (!jarsignerFile.exists()) {
      // This works when a JDK is installed with the JRE.
      jarsignerFile = new File(javaHome + File.separator + ".." + File.separator + "bin" +
          File.separator + "jarsigner");
      if (System.getProperty("os.name").startsWith("Windows")){
  		jarsignerFile = new File(javaHome + File.separator + ".." + File.separator + "bin" +
            File.separator + "jarsigner.exe");
      }
      if (!jarsignerFile.exists()) {
        LOG.warning("YAIL compiler - could not find jarsigner.");
        err.println("YAIL compiler - could not find jarsigner.");
        userErrors.print(String.format(ERROR_IN_STAGE, "JarSigner"));
        return false;
      }
    }

    String[] jarsignerCommandLine = {
        jarsignerFile.getAbsolutePath(),
        "-digestalg", "SHA1",
        "-sigalg", "MD5withRSA",
        "-keystore", keystoreAbsolutePath,
        "-storepass", "android",
        apkAbsolutePath,
        "AndroidKey"
    };
    if (!Execution.execute(null, jarsignerCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - jarsigner execution failed.");
      err.println("YAIL compiler - jarsigner execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "JarSigner"));
      return false;
    }

    return true;
  }

  /*
   * Loads the icon for the application, either a user provided one or the default one.
   */
  private boolean prepareApplicationIcon(File outputPngFile) {
    String userSpecifiedIcon = Strings.nullToEmpty(project.getIcon());
    try {
      BufferedImage icon;
      if (!userSpecifiedIcon.isEmpty()) {
        File iconFile = new File(project.getAssetsDirectory(), userSpecifiedIcon);
        icon = ImageIO.read(iconFile);
        if (icon == null) {
          // This can happen if the iconFile isn't an image file.
          // For example, icon is null if the file is a .wav file.
          // TODO(lizlooney) - This happens if the user specifies a .ico file. We should fix that.
          userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon));
          return false;
        }
      } else {
        // Load the default image.
        icon = ImageIO.read(Compiler.class.getResource(DEFAULT_ICON));
      }
      ImageIO.write(icon, "png", outputPngFile);
    } catch (Exception e) {
      e.printStackTrace();
      // If the user specified the icon, this is fatal.
      if (!userSpecifiedIcon.isEmpty()) {
        userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon));
        return false;
      }
    }

    return true;
  }

  private boolean runDx(File classesDir, String dexedClasses) {
    int mx = childProcessRamMb - 200;
    String[] dxCommandLine = {
        System.getProperty("java.home") + "/bin/java",
        "-mx" + mx + "M",
        "-jar",
        getResource(DX_JAR),
        "--dex",
        "--positions=lines",
        "--output=" + dexedClasses,
        classesDir.getAbsolutePath(),
        getResource(SIMPLE_ANDROID_RUNTIME_JAR),
        getResource(KAWA_RUNTIME),
        getResource(TWITTER_RUNTIME),
    };
    long startDx = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to polute build messages with
    // tools output
    boolean dxSuccess;
    synchronized (SYNC_KAWA_OR_DX) {
      dxSuccess = Execution.execute(null, dxCommandLine, System.out, System.err);
    }
    if (!dxSuccess) {
      LOG.warning("YAIL compiler - DX execution failed.");
      err.println("YAIL compiler - DX execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "DX"));
      return false;
    }
    String dxTimeMessage = "DX time: " +
        ((System.currentTimeMillis() - startDx) / 1000.0) + " seconds";
    out.println(dxTimeMessage);
    LOG.info(dxTimeMessage);

    return true;
  }

  private boolean runAaptPackage(File manifestFile, File resDir, String tmpPackageName) {
    // Need to make sure assets directory exists otherwise aapt will fail.
    createDirectory(project.getAssetsDirectory());
    String aaptTool;
    String osName = System.getProperty("os.name");
    if (osName.equals("Mac OS X")) {
      aaptTool = MAC_AAPT_TOOL;
    } else if (osName.equals("Linux")) {
      aaptTool = LINUX_AAPT_TOOL;
    } else if (osName.startsWith("Windows")) {
		aaptTool = WINDOWS_AAPT_TOOL;
	} else {
      LOG.warning("YAIL compiler - cannot run AAPT on OS " + osName);
      err.println("YAIL compiler - cannot run AAPT on OS " + osName);
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String[] aaptPackageCommandLine = {
        getResource(aaptTool),
        "package",
        "-v",
        "-f",
        "-M", manifestFile.getAbsolutePath(),
        "-S", resDir.getAbsolutePath(),
        "-A", project.getAssetsDirectory().getAbsolutePath(),
        "-I", getResource(ANDROID_RUNTIME),
        "-F", tmpPackageName
    };
    long startAapt = System.currentTimeMillis();
    // Using System.err and System.out on purpose. Don't want to polute build messages with
    // tools output
    if (!Execution.execute(null, aaptPackageCommandLine, System.out, System.err)) {
      LOG.warning("YAIL compiler - AAPT execution failed.");
      err.println("YAIL compiler - AAPT execution failed.");
      userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"));
      return false;
    }
    String aaptTimeMessage = "AAPT time: " +
        ((System.currentTimeMillis() - startAapt) / 1000.0) + " seconds";
    out.println(aaptTimeMessage);
    LOG.info(aaptTimeMessage);

    return true;
  }

  /**
   * Writes out the given resource as a temp file and returns the absolute path.
   * Caches the location of the files, so we can reuse them.
   *
   * @param resourcePath the name of the resource
   */
  static String getResource(String resourcePath) {
    try {
      File file = resources.get(resourcePath);
      if (file == null) {
        String basename = PathUtil.basename(resourcePath);
        String prefix;
        String suffix;
        int lastDot = basename.lastIndexOf(".");
        if (lastDot != -1) {
          prefix = basename.substring(0, lastDot);
          suffix = basename.substring(lastDot);
        } else {
          prefix = basename;
          suffix = "";
        }
        while (prefix.length() < 3) {
          prefix = prefix + "_";
        }
        file = File.createTempFile(prefix, suffix);
        file.setExecutable(true);
        file.deleteOnExit();
        file.getParentFile().mkdirs();
        Files.copy(Resources.newInputStreamSupplier(Compiler.class.getResource(resourcePath)),
            file);
        resources.put(resourcePath, file);
      }
      return file.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void loadComponentPermissions() throws IOException, JSONException {
    synchronized (componentPermissions) {
      if (componentPermissions.isEmpty()) {
        String permissionsJson = Resources.toString(
            Compiler.class.getResource(COMPONENT_PERMISSIONS), Charsets.UTF_8);

        JSONArray componentsArray = new JSONArray(permissionsJson);
        int componentslength = componentsArray.length();
        for (int componentsIndex = 0; componentsIndex < componentslength; componentsIndex++) {
          JSONObject componentObject = componentsArray.getJSONObject(componentsIndex);
          String name = componentObject.getString("name");

          Set<String> permissionsForThisComponent = Sets.newHashSet();

          JSONArray permissionsArray = componentObject.getJSONArray("permissions");
          int permissionsLength = permissionsArray.length();
          for (int permissionsIndex = 0; permissionsIndex < permissionsLength; permissionsIndex++) {
            String permission = permissionsArray.getString(permissionsIndex);
            permissionsForThisComponent.add(permission);
          }

          componentPermissions.put(name, permissionsForThisComponent);
        }
      }
    }
  }

  /**
   * Creates a new directory (if it doesn't exist already).
   *
   * @param dir  new directory
   * @return  new directory
   */
  private static File createDirectory(File dir) {
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }

  /**
   * Creates a new directory (if it doesn't exist already).
   *
   * @param parentDirectory  parent directory of new directory
   * @param name  name of new directory
   * @return  new directory
   */
  private static File createDirectory(File parentDirectory, String name) {
    File dir = new File(parentDirectory, name);
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }
}
