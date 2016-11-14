package tw.com.softleader.dh;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.SimpleAlert;
import tw.com.softleader.dh.basic.TomcatType;
import tw.com.softleader.dh.basic.VerifyException;
import tw.com.softleader.dh.strategy.BackupHandler;
import tw.com.softleader.dh.strategy.DeployHandler;

public class App extends Application {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void start(final Stage stage) {
		stage.setTitle("DeployHelper");

		final Config config = readConfig();

		// ===佈署模組===
		final DeployHandler deployHandler = new DeployHandler(config);
		// ===備份模組===
		final BackupHandler backupHandler = new BackupHandler(config);

		// ===元素準備===
		final CheckBox backupCheckBox = new CheckBox("佈署完成後仍保留備份檔案");

		final ToggleGroup tomcatType = new ToggleGroup();
		final RadioButton rb1 = new RadioButton("bat");
		rb1.setToggleGroup(tomcatType);
		final RadioButton rb2 = new RadioButton("service");
		rb2.setToggleGroup(tomcatType);
		final TextField serviceNameTextField = new TextField();

		final DirectoryChooser deployTomcatChooser = new DirectoryChooser();
		deployTomcatChooser.setTitle("請選擇Tomcat位置");

		final FileChooser warChooser = new FileChooser();
		warChooser.getExtensionFilters().add(new ExtensionFilter("佈署檔", "*.war"));

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

		final Button restoreButton = new Button("還原為指定版本");

		final ListView<String> backupHistory = new ListView<String>();
		backupHistory.setPrefWidth(400);
		backupHistory.setPrefHeight(200);

		// 帶入預設值
		Optional.ofNullable(config.getTomcatType()).ifPresent(type -> {
			if (TomcatType.service.equals(type)) {
				rb2.setSelected(true);
				serviceNameTextField.setVisible(true);
			} else {
				rb1.setSelected(true);
				serviceNameTextField.setVisible(false);
			}
		});
		Optional.ofNullable(config.getTomcatServiceName()).ifPresent(serviceNameTextField::setText);
		Optional.ofNullable(config.isKeepBackUpFile()).ifPresent(backupCheckBox::setSelected);
		Optional.ofNullable(deployHandler.getDeployFile()).map(File::getPath).ifPresent(warPathTextField::setText);
		Optional.ofNullable(deployHandler.getTomcatDir()).map(File::getPath).ifPresent(depolyPathTextField::setText);
		reloadHistory(backupHandler, backupHistory);

		// ===畫面繪製===
		final GridPane propertiesPane = new GridPane();
		GridPane.setConstraints(rb1, 0, 0);
		GridPane.setConstraints(rb2, 1, 0);
		GridPane.setConstraints(serviceNameTextField, 2, 0);
		GridPane.setConstraints(backupCheckBox, 0, 1);
		propertiesPane.setHgap(6);
		propertiesPane.setVgap(6);
		propertiesPane.getChildren().addAll(rb1, rb2, serviceNameTextField, backupCheckBox);

		final GridPane fileChooserPane = new GridPane();
		GridPane.setConstraints(openWarButton, 0, 0);
		GridPane.setConstraints(warPathTextField, 1, 0);
		GridPane.setConstraints(openPathButton, 0, 1);
		GridPane.setConstraints(depolyPathTextField, 1, 1);
		fileChooserPane.setHgap(6);
		fileChooserPane.setVgap(6);
		fileChooserPane.getChildren().addAll(openWarButton, warPathTextField, openPathButton, depolyPathTextField);

		final GridPane backupHistoryPane = new GridPane();
		backupHistoryPane.setHgap(6);
		backupHistoryPane.setVgap(6);
		GridPane.setConstraints(backupHistory, 0, 0);
		backupHistoryPane.getChildren().addAll(backupHistory);

		final GridPane actionPane = new GridPane();
		actionPane.setHgap(6);
		actionPane.setVgap(6);
		GridPane.setConstraints(restoreButton, 0, 0);
		GridPane.setConstraints(deployButton, 1, 0);
		GridPane.setConstraints(logTextField, 2, 0);
		actionPane.getChildren().addAll(restoreButton, deployButton, logTextField);

		final Pane rootGroup = new VBox(12);
		rootGroup.getChildren().addAll(propertiesPane, fileChooserPane, backupHistoryPane, actionPane);
		rootGroup.setPadding(new Insets(12, 12, 12, 12));

		// ===元素事件===
		rb1.setOnAction(e -> {
			serviceNameTextField.setVisible(false);
		});

		rb2.setOnAction(e -> {
			serviceNameTextField.setVisible(true);
		});

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
				deployHandler.setTomcatDir(file);
				backupHandler.setTomcatDir(file);
				reloadHistory(backupHandler, backupHistory);
			}
		});

		deployButton.setOnAction(e -> {
			config.setKeepBackUpFile(backupCheckBox.isSelected());
			config.setTomcatType(rb2.isSelected() ? TomcatType.service : TomcatType.bat);
			try {
				disable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				deployHandler.deploy(
						log -> Platform.runLater(() -> logTextField.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> {
							enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
							reloadHistory(backupHandler, backupHistory);
						}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				ex.printStackTrace();
			}
		});

		restoreButton.setOnAction(e -> {
			config.setTomcatType(rb2.isSelected() ? TomcatType.service : TomcatType.bat);
			backupHandler.setChooseFileName(backupHistory.getSelectionModel().getSelectedItem());
			try {
				disable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				backupHandler.restore(
						log -> Platform.runLater(() -> logTextField.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory)
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(restoreButton, backupCheckBox, openWarButton, openPathButton, deployButton, backupHistory);
				ex.printStackTrace();
			}
		});

		stage.setOnCloseRequest(e -> {
			System.exit(0);
		});

		stage.setScene(new Scene(rootGroup));
		stage.show();
	}

	private Config readConfig() {
		Config config;
		try(InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("config.json")) {
			config = objectMapper.readValue(jsonStream, Config.class);
			config.verify();
		} catch (final VerifyException ex) {
			config = new Config();
			SimpleAlert.warn(ex.getMsgs());
			ex.printStackTrace();
		} catch (final Exception ex) {
			config = new Config();
			SimpleAlert.error("讀取設定檔發生錯誤\n請擷取以下訊息並通報系統管理員", ex);
			ex.printStackTrace();
		}
		return config;
	}

	private void reloadHistory(final BackupHandler backupHandler, final ListView<String> backupHistory) {
		backupHandler.reload();
		Platform.runLater(() -> backupHistory.setItems(FXCollections.observableArrayList(backupHandler.getBackupFiles().stream().map(File::getName).collect(Collectors.toList()))));
	}

	private void disable(final Node... nodes) {
		for (final Node node : nodes) {
			node.setDisable(true);
		}
	}

	private void enable(final Node... nodes) {
		for (final Node node : nodes) {
			node.setDisable(false);
		}
	}

	public static void main(final String[] args) {
		Application.launch(args);
	}

}