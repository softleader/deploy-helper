package tw.com.softleader.dh;

import java.io.File;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import tw.com.softleader.dh.basic.DeployVerifyException;
import tw.com.softleader.dh.basic.SimpleAlert;
import tw.com.softleader.dh.strategy.DeployHandler;

public class App extends Application {

	@Override
	public void start(final Stage stage) {
		System.out.println("start");
		stage.setTitle("DeployHelper");

		// ===佈署模組===
		final DeployHandler deployHandler = new DeployHandler();

		// ===元素準備===
		final DirectoryChooser deployTomcatChooser = new DirectoryChooser();
		deployTomcatChooser.setTitle("請選擇Tomcat位置");

		final FileChooser warChooser = new FileChooser();
		warChooser.getExtensionFilters().add(new ExtensionFilter("佈署檔", "*.war"));

		final CheckBox backupCheckBox = new CheckBox("佈署完成後仍保留備份檔案");

		final Button openWarButton = new Button("選擇要佈署的War檔");
		final TextField warPathTextField = new TextField();
		warPathTextField.setEditable(false);

		final Button openPathButton = new Button("選擇要佈署的路徑");
		final TextField depolyPathTextField = new TextField();
		depolyPathTextField.setEditable(false);
		depolyPathTextField.setPrefWidth(400);

		final Button deployButton = new Button("開始佈署");
		final TextField logTextField = new TextField();
		logTextField.setEditable(false);
		logTextField.setPrefWidth(300);
		logTextField.setText("等待佈署");

		// 帶入預設值
		Optional.ofNullable(deployHandler.isBackup()).ifPresent(backupCheckBox::setSelected);
		Optional.ofNullable(deployHandler.getDeployFile()).map(File::getPath).ifPresent(warPathTextField::setText);
		Optional.ofNullable(deployHandler.getDeployTomcatPath()).map(File::getPath).ifPresent(depolyPathTextField::setText);

		// ===畫面繪製===
		final GridPane propertiesPane = new GridPane();
		GridPane.setConstraints(backupCheckBox, 0, 0);
		propertiesPane.setHgap(6);
		propertiesPane.setVgap(6);
		propertiesPane.getChildren().addAll(backupCheckBox);

		final GridPane fileChooserPane = new GridPane();
		GridPane.setConstraints(openWarButton, 0, 0);
		GridPane.setConstraints(warPathTextField, 1, 0);
		GridPane.setConstraints(openPathButton, 0, 1);
		GridPane.setConstraints(depolyPathTextField, 1, 1);
		fileChooserPane.setHgap(6);
		fileChooserPane.setVgap(6);
		fileChooserPane.getChildren().addAll(openWarButton, warPathTextField, openPathButton, depolyPathTextField);

		final GridPane actionPane = new GridPane();
		actionPane.setHgap(6);
		actionPane.setVgap(6);
		GridPane.setConstraints(deployButton, 0, 0);
		GridPane.setConstraints(logTextField, 1, 0);
		actionPane.getChildren().addAll(deployButton, logTextField);

		final Pane rootGroup = new VBox(12);
		rootGroup.getChildren().addAll(propertiesPane, fileChooserPane, actionPane);
		rootGroup.setPadding(new Insets(12, 12, 12, 12));

		// ===元素事件===
		openWarButton.setOnAction(e -> {
			final File file = warChooser.showOpenDialog(stage);
			if (file != null) {
				warPathTextField.setText(file.getPath());
				deployHandler.setDeployFile(file);
			}
		});

		openPathButton.setOnAction(e -> {
			final File file = deployTomcatChooser.showDialog(stage);
			if (file != null) {
				depolyPathTextField.setText(file.getPath());
				deployHandler.setDeployTomcatPath(file);
			}
		});

		deployButton.setOnAction(e -> {
			deployHandler.setBackup(backupCheckBox.isSelected());
			try {
				disableButtons(backupCheckBox, openWarButton, openPathButton, deployButton);
				deployHandler.deploy(
						log -> Platform.runLater(() -> logTextField.setText(log)),
						() -> enableButtons(backupCheckBox, openWarButton, openPathButton, deployButton)
				);
			} catch (final DeployVerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enableButtons(backupCheckBox, openWarButton, openPathButton, deployButton);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enableButtons(backupCheckBox, openWarButton, openPathButton, deployButton);
				ex.printStackTrace();
			}
		});

		stage.setOnCloseRequest(e -> {
			System.exit(0);
		});

		stage.setScene(new Scene(rootGroup));
		stage.show();
	}

	private void disableButtons(ButtonBase... btns) {
		for (final ButtonBase btn : btns) {
			btn.setDisable(true);
		}
	}

	private void enableButtons(ButtonBase... btns) {
		for (final ButtonBase btn : btns) {
			btn.setDisable(false);
		}
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

}