package com.kpt.adaptxt.beta;

public interface ImeCutCopyPasteInterface {
	public void processImeSelectAll();
	public void processImeCopy();
	public void processImePaste();
	public void processImeCut();
	public void processImeTextChange();
	public void onSelectionChanged(int selStart, int selEnd);
}
