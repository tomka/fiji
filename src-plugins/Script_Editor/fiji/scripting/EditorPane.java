package fiji.scripting;

import fiji.scripting.completion.ClassCompletionProvider;
import fiji.scripting.completion.DefaultProvider;

import java.awt.Container;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;

import org.fife.ui.autocomplete.AutoCompletion;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.RecordableTextAction;
import org.fife.ui.rtextarea.ToolTipSupplier;

public class EditorPane extends RSyntaxTextArea implements DocumentListener {
	TextEditor frame;
	File file;
	long fileLastModified;
	Languages.Language currentLanguage;
	AutoCompletion autocomp;
	ClassCompletionProvider provider;
	Gutter gutter;
	IconGroup iconGroup;
	StartDebugging debugging;
	int modifyCount;
	boolean undoInProgress, redoInProgress;

	public EditorPane(TextEditor frame) {
		this.frame = frame;
		setTabSize(8);
		getActionMap().put(DefaultEditorKit
				.nextWordAction, wordMovement(+1, false));
		getActionMap().put(DefaultEditorKit
				.selectionNextWordAction, wordMovement(+1, true));
		getActionMap().put(DefaultEditorKit
				.previousWordAction, wordMovement(-1, false));
		getActionMap().put(DefaultEditorKit
				.selectionPreviousWordAction, wordMovement(-1, true));
		provider = new ClassCompletionProvider(new DefaultProvider(),
				this, null);
		autocomp = new AutoCompletion(provider);

		autocomp.setListCellRenderer(new CCellRenderer());
		autocomp.setShowDescWindow(true);
		autocomp.setParameterAssistanceEnabled(true);
		autocomp.install(this);
		setToolTipSupplier((ToolTipSupplier)provider);
		ToolTipManager.sharedInstance().registerComponent(this);
		getDocument().addDocumentListener(this);
		currentLanguage = Languages.get("");
	}

	public void embedWithScrollbars(Container container) {
		container.add(embedWithScrollbars());
	}

	public RTextScrollPane embedWithScrollbars() {
		RTextScrollPane sp = new RTextScrollPane(this);
		sp.setPreferredSize(new Dimension(600, 350));
		sp.setIconRowHeaderEnabled(true);
		gutter = sp.getGutter();
		iconGroup = new IconGroup("bullets", "images/", null, "png", null);
		gutter.setBookmarkIcon(iconGroup.getIcon("var"));
		gutter.setBookmarkingEnabled(true);
		return sp;
	}

	RecordableTextAction wordMovement(final int direction,
			final boolean select) {
		final String id = "WORD_MOVEMENT_" + select + direction;
		return new RecordableTextAction(id) {
			public void actionPerformedImpl(ActionEvent e,
					RTextArea textArea) {
				int pos = textArea.getCaretPosition();
				int end = direction < 0 ? 0 :
					textArea.getDocument().getLength();
				while (pos != end && !isWordChar(textArea, pos))
					pos += direction;
				while (pos != end && isWordChar(textArea, pos))
					pos += direction;
				if (select)
					textArea.moveCaretPosition(pos);
				else
					textArea.setCaretPosition(pos);
			}

			public String getMacroID() {
				return id;
			}

			boolean isWordChar(RTextArea textArea, int pos) {
				try {
					char c = textArea.getText(pos
						+ (direction < 0 ? -1 : 0), 1)
						.charAt(0);
					return c > 0x7f ||
						(c >= 'A' && c <= 'Z') ||
						(c >= 'a' && c <= 'z') ||
						(c >= '0' && c <= '9') ||
						c == '_';
				} catch (BadLocationException e) {
					return false;
				}
			}
		};
	}

	public void undoLastAction() {
		undoInProgress = true;
		super.undoLastAction();
		undoInProgress = false;
	}

	public void redoLastAction() {
		redoInProgress = true;
		super.redoLastAction();
		redoInProgress = false;
	}

	public boolean fileChanged() {
		return modifyCount != 0;
	}

	public void insertUpdate(DocumentEvent e) {
		modified();
	}

	public void removeUpdate(DocumentEvent e) {
		modified();
	}

	// triggered only by syntax highlighting
	public void changedUpdate(DocumentEvent e) { }

	protected void modified() {
		checkForOutsideChanges();
		boolean update = modifyCount == 0;
		if (undoInProgress)
			modifyCount--;
		else if (redoInProgress || modifyCount >= 0)
			modifyCount++;
		else // not possible to get back to clean state
			modifyCount = Integer.MIN_VALUE;
		if (update || modifyCount == 0)
			setTitle();
	}

	public void checkForOutsideChanges() {
		if (frame != null && wasChangedOutside() &&
				!frame.reload("The file " + file.getName()
					+ " was changed outside of the editor"))
			fileLastModified = file.lastModified();
	}

	public boolean wasChangedOutside() {
		return file != null && file.exists() &&
				file.lastModified() != fileLastModified;
	}

	public void write(File file) throws IOException {
		BufferedWriter outFile =
			new BufferedWriter(new FileWriter(file));
		outFile.write(getText());
		outFile.close();
		modifyCount = 0;
		fileLastModified = file.lastModified();
	}

	public void setFile(String path) throws IOException {
		file = null;
		if (path == null)
			setText("");
		else {
			File file = new File(path);
			if (!file.exists()) {
				modifyCount = Integer.MIN_VALUE;
				setFileName(file);
				return;
			}
			read(new BufferedReader(new FileReader(file)),
				null);
			this.file = file;
		}
		discardAllEdits();
		modifyCount = 0;
		setFileName(file);
	}

	public void setFileName(File file) {
		this.file = file;
		setTitle();
		if (file != null)
			setLanguageByExtension(getExtension(file.getName()));
		fileLastModified = file == null || !file.exists() ? 0 :
			file.lastModified();
	}

	protected String getFileName() {
		if (file != null)
			return file.getName();
		if (currentLanguage.menuLabel.equals("Java")) {
			String name =
				new TokenFunctions(this).getClassName();
			if (name != null)
				return name + currentLanguage.extension;
		}
		return "New_" + currentLanguage.extension;
	}

	private synchronized void setTitle() {
		if (frame != null)
			frame.setTitle();
	}

	public static String getExtension(String fileName) {
		int dot = fileName.lastIndexOf(".");
		return dot < 0 ?  "" : fileName.substring(dot);
	}

	void setLanguageByExtension(String extension) {
		setLanguage(Languages.get(extension));
	}

	void setLanguage(Languages.Language language) {
		if (language == null)
			language = Languages.get("");

		if (file != null) {
			String name = file.getName();
			if (!name.endsWith(language.extension) &&
					currentLanguage != null) {
				String ext = currentLanguage.extension;
				if (name.endsWith(ext))
					name = name.substring(0, name.length()
							- ext.length());
				file = new File(file.getParentFile(),
						name + language.extension);
				modifyCount = Integer.MIN_VALUE;
			}
		}
		currentLanguage = language;

		provider.setProviderLanguage(language.menuLabel);

		// TODO: these should go to upstream RSyntaxTextArea
		if (language.syntaxStyle != null)
			setSyntaxEditingStyle(language.syntaxStyle);
		else if (language.extension.equals(".clj"))
			getRSyntaxDocument()
				.setSyntaxStyle(new ClojureTokenMaker());
		else if (language.extension.equals(".m"))
			getRSyntaxDocument()
				.setSyntaxStyle(new MatlabTokenMaker());

		frame.setTitle();
		frame.updateLanguageMenu(language);
	}

	protected RSyntaxDocument getRSyntaxDocument() {
		return (RSyntaxDocument)getDocument();
	}

	public ClassNameFunctions getClassNameFunctions() {
		return new ClassNameFunctions(provider);
	}

	public void startDebugging() {
		if (currentLanguage == null ||
					!currentLanguage.isDebuggable())
			throw new RuntimeException("Debugging unavailable");

		BreakpointManager manager = new BreakpointManager(gutter, this, iconGroup);
		debugging = new StartDebugging(file.getPath(), manager.findBreakpointsLineNumber());

		try {
			System.out.println(debugging.startDebugging().exitValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resume() {
		debugging.resumeVM();
	}

	public void terminate() {
		throw new RuntimeException("TODO: unimplemented!");
	}
}
