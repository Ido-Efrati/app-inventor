// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.appinventor.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Tree.Resources;

/**
 * Image bundle containing all client images.
 *
 * Note: Images extends Tree.Resources rather than ClientBundle so that
 * the Images can be used with the com.google.gwt.user.client.ui.Tree class.
 *
 */
public interface Images extends Resources {

  /**
   * Close button image for the box widget
   */
  @Source("com/google/appinventor/images/boxClose.png")
  ImageResource boxClose();

  /**
   * Menu button image for the box widget
   */
  @Source("com/google/appinventor/images/boxMenu.png")
  ImageResource boxMenu();

  /**
   * Minimize button image for the box widget
   */
  @Source("com/google/appinventor/images/boxMinimize.png")
  ImageResource boxMinimize();

  /**
   * Restore button image for the box widget
   */
  @Source("com/google/appinventor/images/boxRestore.png")
  ImageResource boxRestore();

  /**
   * Close button image for the tab widget
   */
  @Source("com/google/appinventor/images/close.png")
  ImageResource close();

  /**
   * Phone status bar shown above the form in the visual designer
   */
  @Source("com/google/appinventor/images/phonebar.png")
  ImageResource phonebar();

  /**
   * Designer palette item: question mark for more component information
   */
  @Source("com/google/appinventor/images/help.png")
  ImageResource help();

  /**
   * Designer palette item: accelerometersensor component
   */
  @Source("com/google/appinventor/images/accelerometersensor.png")
  ImageResource accelerometersensor();

  /**
   * Designer palette item: barcode scanner component
   */
  @Source("com/google/appinventor/images/barcodeScanner.png")
  ImageResource barcodeScanner();

  /**
   * Designer palette item: button component
   */
  @Source("com/google/appinventor/images/button.png")
  ImageResource button();

  /**
   * Designer palette item: camera declaration
   */
  @Source("com/google/appinventor/images/camera.png")
  ImageResource camera();
  
  /**
   * Designer palette item: camcorder declaration
   */
  @Source("com/google/appinventor/images/camcorder.png")
  ImageResource camcorder();

  /**
   * Designer palette item: canvas component
   */
  @Source("com/google/appinventor/images/canvas.png")
  ImageResource canvas();

  /**
   * Designer palette item: checkbox component
   */
  @Source("com/google/appinventor/images/checkbox.png")
  ImageResource checkbox();

  /**
   * Designer palette item: form component
   */
  @Source("com/google/appinventor/images/form.png")
  ImageResource form();

  /**
   * Designer palette item: horizontal arrangement component
   */
  @Source("com/google/appinventor/images/horizontal.png")
  ImageResource horizontal();

  /**
   * Designer palette item: image component
   * Also used for image file icon for project explorer
   */
  @Source("com/google/appinventor/images/image.png")
  ImageResource image();

  /**
   * Designer palette item: label component
   */
  @Source("com/google/appinventor/images/label.png")
  ImageResource label();

  /**
   * Designer palette item: listbox component
   */
  @Source("com/google/appinventor/images/listbox.png")
  ImageResource listbox();

  /**
   * Designer palette item: orientationsensor component
   */
  @Source("com/google/appinventor/images/orientationsensor.png")
  ImageResource orientationsensor();

  /**
   * Designer palette item: player component
   */
  @Source("com/google/appinventor/images/player.png")
  ImageResource player();

  /**
   * Designer palette item: sound recorder component
   */
  @Source("com/google/appinventor/images/soundRecorder.png")
  ImageResource soundRecorder();

  /**
   * Designer palette item: VideoPlayer component
   */
  @Source("com/google/appinventor/images/videoPlayer.png")
  ImageResource videoplayer();

  /**
   * Designer palette item: progressbar component
   */
  @Source("com/google/appinventor/images/progressbar.png")
  ImageResource progressbar();

  /**
   * Designer palette item: radiobutton component
   */
  @Source("com/google/appinventor/images/radiobutton.png")
  ImageResource radiobutton();

  /**
   * Designer palette item: textbox component
   */
  @Source("com/google/appinventor/images/textbox.png")
  ImageResource textbox();

  /**
   * Designer palette item: PasswordTextBox component.
   */
  @Source("com/google/appinventor/images/passwordtextbox.png")
  ImageResource passwordtextbox();

  /**
   * Designer palette item: clock component
   */
  @Source("com/google/appinventor/images/clock.png")
  ImageResource clock();

  /**
   * Designer palette item: SoundEffect component
   */
  @Source("com/google/appinventor/images/soundEffect.png")
  ImageResource soundeffect();

  /**
   * Designer palette item: ContactPicker component
   */
  @Source("com/google/appinventor/images/contactPicker.png")
  ImageResource contactpicker();

  /**
   * Designer palette item: PhoneNumberPicker component
   */
  @Source("com/google/appinventor/images/phoneNumberPicker.png")
  ImageResource phonenumberpicker();

  /**
   * Designer palette item: ImagePicker component
   */
  @Source("com/google/appinventor/images/imagePicker.png")
  ImageResource imagepicker();

  /**
   * Designer palette item: ListPicker component
   */
  @Source("com/google/appinventor/images/listPicker.png")
  ImageResource listpicker();

  /**
   * Designer palette item: PhoneCall component
   */
  @Source("com/google/appinventor/images/phoneCall.png")
  ImageResource phonecall();

  /**
   * Designer palette item: ActivityStarter component
   */
  @Source("com/google/appinventor/images/activityStarter.png")
  ImageResource activitystarter();

  /**
   * Designer palette item: EmailPicker component
   */
  @Source("com/google/appinventor/images/emailPicker.png")
  ImageResource emailpicker();

  /**
   * Designer palette item: Texting component
   */
  @Source("com/google/appinventor/images/texting.png")
  ImageResource texting();

  /**
   * Designer palette item: GameClient component
   */
  @Source("com/google/appinventor/images/gameClient.png")
  ImageResource gameclient();

  /**
   * Designer palette item: Sprite
   */
  @Source("com/google/appinventor/images/imageSprite.png")
  ImageResource imageSprite();

  /**
   * Designer palette item: Ball
   */
  @Source("com/google/appinventor/images/ball.png")
  ImageResource ball();

  /**
   * Designer palette item: Notifier
   */
  @Source("com/google/appinventor/images/notifier.png")
  ImageResource notifier();

  /**
   * Designer palette item: LocationSensor
   */
  @Source("com/google/appinventor/images/locationSensor.png")
  ImageResource locationSensor();

  /**
   * Designer palette item: SpeechRecognizer component
   */
  @Source("com/google/appinventor/images/speechRecognizer.png")
  ImageResource speechRecognizer();

  /**
   * Designer palette item: table arrangement component
   */
  @Source("com/google/appinventor/images/table.png")
  ImageResource table();

  /**
   * Designer palette item: Twitter Component
   */
  @Source("com/google/appinventor/images/twitter.png")
  ImageResource twitterComponent();

  /**
   * Designer palette item: TinyDB Component
   */
  @Source("com/google/appinventor/images/tinyDB.png")
  ImageResource tinyDB();

  /**
   * Designer palette item: TinyWebDB Component
   */
  @Source("com/google/appinventor/images/tinyWebDB.png")
  ImageResource tinyWebDB();

  /**
   * Designer palette item: TextToSpeech component
   */
  @Source("com/google/appinventor/images/textToSpeech.png")
  ImageResource textToSpeech();

  /**
   * Designer palette item: vertical arrangement component
   */
  @Source("com/google/appinventor/images/vertical.png")
  ImageResource vertical();

  /**
   * Designer palette item: VotingComponent
   */
  @Source("com/google/appinventor/images/voting.png")
  ImageResource voting();

  /**
   * Designer palette item: Pedometer Component
   */
  @Source("com/google/appinventor/images/pedometer.png")
  ImageResource pedometerComponent();

  /**
   * Designer palette item: Lego Mindstorms NXT components
   */
  @Source("com/google/appinventor/images/legoMindstormsNxt.png")
  ImageResource legoMindstormsNxt();

  /**
   * Designer palette item: Bluetooth components
   */
  @Source("com/google/appinventor/images/bluetooth.png")
  ImageResource bluetooth();

  /**
   * Designer palette item: FusiontablesControl component
   */
  @Source("com/google/appinventor/images/fusiontables.png")
  ImageResource fusiontables();

  /**
   * Designer palette item: WebViewer component
   */
  @Source("com/google/appinventor/images/webviewer.png")
  ImageResource webviewer();

  /**
   * Designer item: WebViewer component in designer
   */
  // The image here is public domain and comes from
  // www.pdclipart.org/displayimage.php/?pid=1047
  @Source("com/google/appinventor/images/webviewerbig.png")
  ImageResource webviewerbig();

  /**
   * Designer palette item: Web component
   */
  @Source("com/google/appinventor/images/web.png")
  ImageResource web();
}
