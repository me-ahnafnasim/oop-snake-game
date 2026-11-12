package gui;

import exception.InvalidRuleException;
import game.PlayerRule;
import game.RuleEngine;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Lets the player write, edit, enable, disable and delete their own .rule files. */
final class RuleManagerDialog {
    private static final String ENABLED = ".rule";
    private static final String DISABLED = ".rule.disabled";
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String TEMPLATE =
            "RULE MyRule PRIORITY 10 WHEN event == FOOD_COLLECTED AND score >= 50 DO add_score(5)";

    /** One rule file as listed; {@code rule} is null when the file does not parse. */
    private record Entry(Path path, boolean enabled, PlayerRule rule, String error) {
        String fileName() {
            return path.getFileName().toString();
        }

        @Override
        public String toString() {
            String state = enabled ? "ON " : "OFF";
            return rule == null
                    ? state + "   invalid   " + fileName() + "   (" + error + ")"
                    : state + "   P" + rule.priority() + "   " + rule.name() + "   (" + fileName() + ")";
        }
    }

    private final Path directory;
    private final RuleEngine parser = new RuleEngine();
    private final ListView<Entry> list = new ListView<>();

    RuleManagerDialog(Path directory) {
        this.directory = directory;
    }

    void showAndWait() {
        Button add = new Button("New…");
        Button edit = new Button("Edit…");
        Button toggle = new Button("Enable / Disable");
        Button delete = new Button("Delete");
        add.setOnAction(e -> createRule());
        edit.setOnAction(e -> selected().ifPresent(this::editRule));
        toggle.setOnAction(e -> selected().ifPresent(this::toggleRule));
        delete.setOnAction(e -> selected().ifPresent(this::deleteRule));
        BooleanBinding nothingSelected = list.getSelectionModel().selectedItemProperty().isNull();
        edit.disableProperty().bind(nothingSelected);
        toggle.disableProperty().bind(nothingSelected);
        delete.disableProperty().bind(nothingSelected);

        Label help = new Label("Grammar: RULE <name> PRIORITY <n> WHEN <condition> DO <action>; <action>\n"
                + "Lower priority numbers run first. Disabled rules stay on disk but are not loaded.");
        help.setWrapText(true);
        list.setPrefSize(640, 260);
        VBox content = new VBox(10, list, new HBox(8, add, edit, toggle, delete), help);
        content.setPadding(new Insets(10));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rule Manager");
        dialog.setHeaderText("Your rules, in the order they run");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        refresh();
        dialog.showAndWait();
    }

    private Optional<Entry> selected() {
        return Optional.ofNullable(list.getSelectionModel().getSelectedItem());
    }

    private void refresh() {
        List<Entry> entries = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (var files = Files.list(directory)) {
                for (Path path : files.toList()) {
                    String name = path.getFileName().toString();
                    if (name.endsWith(ENABLED) || name.endsWith(DISABLED)) entries.add(read(path, name.endsWith(ENABLED)));
                }
            } catch (IOException e) {
                error("Cannot list rules", e.getMessage());
            }
        }
        entries.sort(Comparator.comparing((Entry entry) -> entry.rule() == null)
                .thenComparingInt(entry -> entry.rule() == null ? 0 : entry.rule().priority())
                .thenComparing(Entry::fileName));
        list.getItems().setAll(entries);
    }

    private Entry read(Path path, boolean enabled) {
        try {
            return new Entry(path, enabled, parser.parse(Files.readString(path)), null);
        } catch (IOException e) {
            return new Entry(path, enabled, null, "cannot be read");
        } catch (InvalidRuleException e) {
            return new Entry(path, enabled, null, e.getMessage());
        }
    }

    private void createRule() {
        TextInputDialog ask = new TextInputDialog("my_rule");
        ask.setTitle("New rule");
        ask.setHeaderText("File name: letters, digits, _ or -");
        ask.setContentText("Name:");
        Optional<String> name = ask.showAndWait().map(String::trim);
        if (name.isEmpty()) return;
        if (!FILE_NAME.matcher(name.get()).matches()) {
            error("Invalid file name", "Use only letters, digits, _ and -.");
            return;
        }
        Path target = directory.resolve(name.get() + ENABLED);
        if (Files.exists(target) || Files.exists(directory.resolve(name.get() + DISABLED))) {
            error("Rule already exists", "A rule file named " + name.get() + " already exists.");
            return;
        }
        editText("New rule", TEMPLATE).ifPresent(text -> save(target, text));
    }

    private void editRule(Entry entry) {
        try {
            String current = Files.readString(entry.path());
            editText("Edit " + entry.fileName(), current).ifPresent(text -> save(entry.path(), text));
        } catch (IOException e) {
            error("Cannot read rule", e.getMessage());
        }
    }

    /** Disabling renames name.rule to name.rule.disabled, so the loader skips it; enabling renames it back. */
    private void toggleRule(Entry entry) {
        String suffix = entry.enabled() ? ENABLED : DISABLED;
        String base = entry.fileName().substring(0, entry.fileName().length() - suffix.length());
        try {
            Files.move(entry.path(), directory.resolve(base + (entry.enabled() ? DISABLED : ENABLED)));
        } catch (IOException e) {
            error("Cannot change rule", e.getMessage());
        }
        refresh();
    }

    private void deleteRule(Entry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + entry.fileName() + "? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
        try {
            Files.delete(entry.path());
        } catch (IOException e) {
            error("Cannot delete rule", e.getMessage());
        }
        refresh();
    }

    private void save(Path target, String text) {
        try {
            Files.createDirectories(directory);
            Files.writeString(target, text.strip() + System.lineSeparator());
        } catch (IOException e) {
            error("Cannot save rule", e.getMessage());
        }
        refresh();
    }

    /** A text editor whose OK button only closes the window once the text is a valid rule. */
    private Optional<String> editText(String title, String initial) {
        TextArea area = new TextArea(initial);
        area.setWrapText(true);
        area.setPrefRowCount(5);
        area.setPrefColumnCount(60);
        Label message = new Label();
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: firebrick;");

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Lines starting with # are comments. The first other line is the rule.");
        dialog.getDialogPane().setContent(new VBox(8, area, message));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                parser.parse(area.getText());
            } catch (InvalidRuleException e) {
                message.setText(e.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? area.getText() : null);
        return dialog.showAndWait();
    }

    private static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
