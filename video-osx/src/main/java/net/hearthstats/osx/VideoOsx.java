package net.hearthstats.osx;

import org.rococoa.NSClass;
import org.rococoa.NSObject;
import org.rococoa.Rococoa;

public interface VideoOsx extends NSObject {

  public abstract int findProgramPid();

  public abstract String getHSWindowBounds();

  public abstract void startVideo();

  public abstract String stopVideo();

  public static final _Class CLASS = Rococoa.createClass("VideoOsx", _Class.class);

  public interface _Class extends NSClass {
    VideoOsx alloc();
  }

}