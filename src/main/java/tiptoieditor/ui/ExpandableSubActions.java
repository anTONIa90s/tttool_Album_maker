package tiptoieditor.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A collapsed-by-default titled pane with padded content.
 */
public class ExpandableSubActions extends TitledPane {

    private final Button loadButton;
    private final Label selectedItemLabel;
    private final Button actionButton;

    public ExpandableSubActions(String title, Button loadButton, Label selectedItemLabel,
                                Button actionButton) {
        super(title, createPaddedContent(loadButton, selectedItemLabel, actionButton));
        this.loadButton = loadButton;
        this.selectedItemLabel = selectedItemLabel;
        this.actionButton = actionButton;
        setExpanded(false);
    }

    private static VBox createPaddedContent(Button loadButton, Label selectedItemLabel,
                                            Button actionButton) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10, loadButton, selectedItemLabel, spacer, actionButton);
        controls.setMaxWidth(Double.MAX_VALUE);

        VBox contentBox = new VBox(controls);
        contentBox.setPadding(new Insets(10));
        return contentBox;
    }

    public Button getLoadButton() {
        return loadButton;
    }

    public Label getSelectedItemLabel() {
        return selectedItemLabel;
    }

    public Button getActionButton() {
        return actionButton;
    }
}
