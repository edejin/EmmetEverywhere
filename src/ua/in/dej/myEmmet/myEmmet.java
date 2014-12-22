package ua.in.dej.myEmmet;

//import com.intellij.ide.impl.DataManagerImpl;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.project.Project;
//import sun.org.mozilla.javascript.internal.NativeObject; // 1.7

import javax.script.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fima on 28.04.14.
 */
public class myEmmet extends AnAction {

    private static Invocable myInv = null;
    private static Boolean Once = false;

    public myEmmet() {

        Notify();

        ScriptEngineManager factory = null;
        ScriptEngine engine = null;
//        if (System.getProperty("java.version").indexOf("1.8") == 0) {
            try {
                factory = new ScriptEngineManager();
                engine = factory.getEngineByMimeType("application/javascript");
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                if (engine == null) {
                    factory = new ScriptEngineManager(null);
                    engine = factory.getEngineByMimeType("application/javascript");
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                String theString = "";
                theString = getStringFromInputStream(this.getClass().getResourceAsStream("/emmet.js"));
                engine.eval(theString);
                myInv = (Invocable) engine;
            } catch (Throwable e) {
                e.printStackTrace();
            }
//        } else {
//            badJavaVersion();
//        }
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    public void Notify() {
        if (!Once) {
            Once = true;
            Notifications.Bus.notify(new Notification("EmmetEverywhere",
                    "EmmetEverywhere plugin updated to v1.2.4",
                    "If you find any bug send your OS and IDEA type and version, Java version and EmmetEverywhere version.<br />" +
                            "Don't forget about stacktrace!<br />" +
                            "<a href=\"mailto:efim@dej.in.ua\">efim@dej.in.ua</a><br />" +
                            "If you find my plugin helpful, donate me using <b><a href=\"https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=C5VR7Y8A5EXBU\">PayPal</a></b>",
                    NotificationType.INFORMATION,
                    NotificationListener.URL_OPENING_LISTENER));
        }
    }

    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getProject();
        FileEditor fileEditor = event.getData(PlatformDataKeys.FILE_EDITOR);
        Editor editor = ((PsiAwareTextEditorImpl) fileEditor).getEditor();
        Document document = editor.getDocument();

        if (document.isWritable()) {
            String fullText = document.getText();
            CaretModel caretModel = editor.getCaretModel();
            Integer caretPosition = caretModel.getOffset();
            Integer lineStart = caretModel.getVisualLineStart();
            String leftValue = fullText.substring(lineStart, caretPosition);
            Boolean rightGood = false;

            if (fullText.length() == caretModel.getOffset()) {
                rightGood = true;
            } else {
                rightGood = fullText.substring(caretPosition, caretPosition + 1).matches("\\s");
            }

            if (caretModel.getOffset() == 0) {
                rightGood = false;
            } else {
                rightGood = rightGood && (fullText.substring(caretPosition - 1, caretPosition).matches("\\S"));
            }
//            NativeObject outputData = null; // 1.7
            Integer selectStart = 0;
            Integer selectStop = 0;
            String value = null;

            if (rightGood) {
                String valueForEmmet = "";
                Integer i = caretPosition;
                Boolean inBrace = false;
                Boolean inSquare = false;

                for (; i > lineStart &&
                        ((inBrace || inSquare)
                                || (!inBrace && !inSquare && fullText.substring(i - 1, i).matches("\\S")))
                        ; i--) {

                    if (!inSquare && !inBrace && fullText.substring(i - 1, i).equals("}")) {
                        inBrace = true;
                    }
                    if (inBrace && fullText.substring(i - 1, i).equals("{")) {
                        inBrace = false;
                    }
                    if (!inSquare && !inBrace && fullText.substring(i - 1, i).equals("]")) {
                        inSquare = true;
                    }
                    if (inSquare && fullText.substring(i - 1, i).equals("[")) {
                        inSquare = false;
                    }
                    valueForEmmet = fullText.substring(i - 1, i) + valueForEmmet;
                }

                Object tmp = null;

                try {
                    // 1.7
                    tmp = myInv.invokeFunction("job", valueForEmmet, caretPosition);
//                    if (tmp instanceof NativeObject) {
//                        outputData = (NativeObject) tmp;
//                    } else {
//                        throw new Exception("tmp is type: " + tmp.getClass().getName());
//                    }
                    // 1.8
                    Matcher matcher = Pattern.compile("^((.|\\s)*)\\\",\\\"selectStart\\\":(.*),\\\"selectStop\\\":(.*)$", Pattern.MULTILINE).matcher((String) tmp);
                    while (matcher.find()) {
                        value = (String) matcher.group(1);
                        selectStart = Integer.parseInt(matcher.group(3));
                        selectStop = Integer.parseInt(matcher.group(4));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
//                System.out.print(resultText);
                if (value != null) {
                    try {
                        final SelectionModel selectionModel = editor.getSelectionModel();
                        final Document documentF = document;
                        final Integer iF = i;
                        final Integer caretOffsetF = caretModel.getOffset();
                        // 1.7
//                    final String resultStringF = (String) outputData.get("text", null);
//                    final Integer startSelection = ((Double) outputData.get("selectStart", null)).intValue();
//                    final Integer stopSelection = ((Double) outputData.get("selectStop", null)).intValue();
                        // 1.8
                        final String resultStringF = value;
                        final Integer startSelection = selectStart;
                        final Integer stopSelection = selectStop;
                        // end if :)
                        final CaretModel caretModelF = caretModel;
                        final Runnable readRunner = new Runnable() {
                            @Override
                            public void run() {
                                documentF.replaceString(iF, caretOffsetF, resultStringF);
                                selectionModel.setSelection(startSelection, stopSelection);
                                caretModelF.moveToOffset(stopSelection);
                            }
                        };
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                                    @Override
                                    public void run() {
                                        ApplicationManager.getApplication().runWriteAction(readRunner);
                                    }
                                }, "DiskRead", null);
                            }
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

