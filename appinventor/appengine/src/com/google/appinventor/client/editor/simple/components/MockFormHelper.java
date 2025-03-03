// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.appinventor.client.editor.simple.components;

/**
 * Helper class for MockForm to get around a restriction in the
 * Java compiler on calling super
 *
 * @author hal@mit.edu (Hal Abelson)
 */
public class MockFormHelper {

  // This class will be called each time we create a form.  We add synchronization now to protect
  // saveLayout, even though there should not be more than one thread creating layouts.
  // but who knows what we might do in the future.


  private static MockFormLayout saveLayout;

  public static synchronized MockFormLayout makeLayout() {
    saveLayout = new MockFormLayout();
    return saveLayout;
  }

  public static synchronized MockFormLayout getLayout() {
    return saveLayout;
  }

}

