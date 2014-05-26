package net.hearthstats.win.jna.extra;

import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinGDI;

public interface WinGDIExtra extends WinGDI {

	public DWORD SRCCOPY = new DWORD(0x00CC0020);

}