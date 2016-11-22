package tw.com.softleader.dh;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.controlsfx.control.MaskerPane;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
import tw.com.softleader.dh.strategy.RemarkHandler;

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
		// ===註記模組===
		final RemarkHandler remarkHandler = new RemarkHandler(config);

		// ===元素準備===
		final CheckBox backupCheckBox = new CheckBox("佈署完成後仍保留備份檔案");

		final ToggleGroup tomcatType = new ToggleGroup();
		final List<RadioButton> rbs = Stream.of(TomcatType.values()).map(type -> new RadioButton(type.toString())).collect(Collectors.toList());
		rbs.forEach(rb -> rb.setToggleGroup(tomcatType));
		final TextField serviceNameTextField = new TextField();

		final DirectoryChooser deployTomcatChooser = new DirectoryChooser();
		deployTomcatChooser.setTitle("請選擇Tomcat位置");

		final FileChooser warChooser = new FileChooser();
		warChooser.getExtensionFilters().add(new ExtensionFilter("佈署檔", "*.war"));

		final Button openWarButton = new Button("選擇要佈署的War檔");
		openWarButton.setPrefWidth(200);
		final TextField warPathTextField = new TextField();
		warPathTextField.setEditable(false);
		warPathTextField.setPrefWidth(400);

		final Button openPathButton = new Button("選擇要佈署的路徑");
		openPathButton.setPrefWidth(200);
		final TextField depolyPathTextField = new TextField();
		depolyPathTextField.setEditable(false);
		depolyPathTextField.setPrefWidth(400);

		final Button deployButton = new Button("開始佈署");
		deployButton.setDefaultButton(true);

		final Button restoreButton = new Button("還原為指定版本");

		final ListView<String> backupHistory = new ListView<String>();
		backupHistory.setPrefWidth(400);
		backupHistory.setPrefHeight(200);

		final TextArea backupRemark = new TextArea();
		backupRemark.setPrefWidth(350);
		backupRemark.setPrefHeight(200);
		backupRemark.setDisable(true);

		final Button backupRemarkButton = new Button("儲存(ctrl+s)");
		backupRemarkButton.setPrefWidth(350);

		// ===畫面繪製===
		final GridPane propertiesPane = new GridPane();
		final List<Node> rbNodes = new ArrayList<>();
		for (int i = 0; i < rbs.size(); i++) {
			if (TomcatType.service.toString().equals(rbs.get(i).getText())) {
				final GridPane serviceTypePane = new GridPane();
				GridPane.setConstraints(rbs.get(i), 0, 0);
				GridPane.setConstraints(serviceNameTextField, 1, 0);
				serviceTypePane.getChildren().addAll(rbs.get(i), serviceNameTextField);
				GridPane.setConstraints(serviceTypePane, i, 0);
				rbNodes.add(serviceTypePane);
			} else {
				GridPane.setConstraints(rbs.get(i), i, 0);
				rbNodes.add(rbs.get(i));
			}
		}

		GridPane.setConstraints(backupCheckBox, 0, 1);
		propertiesPane.setHgap(6);
		propertiesPane.setVgap(6);
		propertiesPane.getChildren().addAll(rbNodes);
		propertiesPane.getChildren().addAll(backupCheckBox);

		final GridPane fileChooserPane = new GridPane();
		GridPane.setConstraints(openWarButton, 0, 0);
		GridPane.setConstraints(warPathTextField, 1, 0);
		GridPane.setConstraints(openPathButton, 0, 1);
		GridPane.setConstraints(depolyPathTextField, 1, 1);
		fileChooserPane.setHgap(6);
		fileChooserPane.setVgap(6);
		fileChooserPane.getChildren().addAll(openWarButton, warPathTextField, openPathButton, depolyPathTextField);


		final GridPane backupRemarkPane = new GridPane();
		backupRemarkPane.setHgap(6);
		backupRemarkPane.setVgap(6);
		GridPane.setConstraints(backupRemark, 0, 0);
		GridPane.setConstraints(backupRemarkButton, 0, 1);
		backupRemarkPane.getChildren().addAll(backupRemark, backupRemarkButton);


		final GridPane backupHistoryPane = new GridPane();
		backupHistoryPane.setHgap(6);
		backupHistoryPane.setVgap(6);
		GridPane.setConstraints(backupHistory, 0, 0);
		GridPane.setConstraints(backupRemarkPane, 1, 0);
		backupHistoryPane.getChildren().addAll(backupHistory, backupRemarkPane);

		final GridPane actionPane = new GridPane();
		actionPane.setHgap(6);
		actionPane.setVgap(6);
		GridPane.setConstraints(restoreButton, 0, 0);
		GridPane.setConstraints(deployButton, 1, 0);
		actionPane.getChildren().addAll(restoreButton, deployButton);

		final MaskerPane masker = new MaskerPane();
		masker.setVisible(false);
		final Pane root = new VBox(12);
		root.getChildren().addAll(propertiesPane, fileChooserPane, backupHistoryPane, actionPane);
		root.setPadding(new Insets(12, 12, 12, 12));
		final StackPane body = new StackPane(root, masker);

		// ===元素事件===
		rbs.forEach(rb -> {
			if (TomcatType.service.toString().equals(rb.getText())) {
				rb.setOnAction(e -> {
					serviceNameTextField.setVisible(true);
					config.setTomcatType(TomcatType.valueOf(rb.getText()));
				});
			} else {
				rb.setOnAction(e -> {
					serviceNameTextField.setVisible(false);
					config.setTomcatType(TomcatType.valueOf(rb.getText()));
				});
			}
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
				remarkHandler.setTomcatDir(file);
				reloadHistory(backupHandler, backupHistory);
			}
		});

		deployButton.setOnAction(e -> {
			config.setKeepBackUpFile(backupCheckBox.isSelected());
			try {
				masker.setText("準備開始進行佈署...");
				disable(masker);
				deployHandler.deploy(
						log -> Platform.runLater(() -> masker.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> {
							enable(masker);
							reloadHistory(backupHandler, backupHistory);
						}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		restoreButton.setOnAction(e -> {
			backupHandler.setChooseFileName(backupHistory.getSelectionModel().getSelectedItem());
			try {
				masker.setText("準備開始進行還原...");
				disable(masker);
				backupHandler.restore(
						log -> Platform.runLater(() -> masker.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> enable(masker)
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		backupHistory.setOnMouseClicked(e -> {
			try {
				masker.setText("讀取備份註記中...");
				disable(masker, backupRemarkButton, backupRemark);
				remarkHandler.loadRemark(
					e,
					t -> Platform.runLater(() -> SimpleAlert.error("讀取註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
					text -> Platform.runLater(() -> {
						backupRemark.setText(text);
						enable(masker, backupRemarkButton, backupRemark);
					}),
					() -> enable(masker)
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("讀取註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		backupRemarkButton.setOnAction(e -> {
			try {
				masker.setText("儲存備份註記中...");
				disable(masker, backupRemarkButton, backupRemark);
				remarkHandler.saveRemark(
					backupRemark.getText(),
					backupHistory.getSelectionModel().getSelectedItem(),
					t -> Platform.runLater(() -> {
						SimpleAlert.error("儲存註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t);
						enable(masker, backupRemarkButton, backupRemark);
					}),
					() -> {}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker, backupRemarkButton, backupRemark);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("儲存註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker, backupRemarkButton, backupRemark);
				ex.printStackTrace();
			}
		});

		backupRemark.setOnKeyPressed(e -> {
			if (e.isShortcutDown() && e.isControlDown() && KeyCode.S.equals(e.getCode())) {
				backupRemarkButton.fire();
			}
		});

		stage.setOnCloseRequest(e -> {
			System.exit(0);
		});

		// 帶入預設值
		Optional.ofNullable(config.getTomcatType()).ifPresent(type -> {
			rbs.forEach(rb -> {
				if (type.toString().equals(rb.getText())) {
					rb.setSelected(true);
					rb.fire();
				}
			});
		});
		Optional.ofNullable(config.getTomcatServiceName()).ifPresent(serviceNameTextField::setText);
		Optional.ofNullable(config.isKeepBackUpFile()).ifPresent(backupCheckBox::setSelected);
		Optional.ofNullable(deployHandler.getDeployFile()).map(File::getPath).ifPresent(warPathTextField::setText);
		Optional.ofNullable(deployHandler.getTomcatDir()).map(File::getPath).ifPresent(depolyPathTextField::setText);
		reloadHistory(backupHandler, backupHistory);

		final Scene scene = new Scene(body);
		scene.getStylesheets().add(getClass().getClassLoader().getResource("application.css").toExternalForm());
		stage.setScene(scene);
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

	private void disable(final MaskerPane masker, final Node... nodes) {
		masker.setVisible(true);
		for (final Node node : nodes) {
			node.setDisable(true);
		}
	}

	private void enable(final MaskerPane masker, final Node... nodes) {
		masker.setVisible(false);
		for (final Node node : nodes) {
			node.setDisable(false);
		}
	}

	public static void main(final String[] args) {
		Application.launch(args);
	}

}