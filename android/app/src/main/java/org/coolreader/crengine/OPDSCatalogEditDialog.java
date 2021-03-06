package org.coolreader.crengine;

import org.coolreader.R;
import org.coolreader.crengine.filebrowser.FileBrowserActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import cn.cc.ereader.MainActivity;

public class OPDSCatalogEditDialog extends BaseDialog {

	private final BaseActivity mActivity;
	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final EditText nameEdit;
	private final EditText urlEdit;
	private final EditText usernameEdit;
	private final EditText passwordEdit;
	private final Runnable mOnUpdate;

	public OPDSCatalogEditDialog(BaseActivity activity, FileInfo item, Runnable onUpdate) {
		super(activity, activity.getString((item.id == null) ? R.string.dlg_catalog_add_title
				: R.string.dlg_catalog_edit_title), true,
				false);
		mActivity = activity;
		mItem = item;
		mOnUpdate = onUpdate;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.catalog_edit_dialog, null);
		nameEdit = view.findViewById(R.id.catalog_name);
		urlEdit = view.findViewById(R.id.catalog_url);
		usernameEdit = view.findViewById(R.id.catalog_username);
		passwordEdit = view.findViewById(R.id.catalog_password);
		nameEdit.setText(mItem.filename);
		urlEdit.setText(mItem.getOPDSUrl());
		usernameEdit.setText(mItem.username);
		passwordEdit.setText(mItem.password);
		setThirdButtonImage(R.drawable.cr3_button_remove, R.string.mi_catalog_delete);
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		String url = urlEdit.getText().toString();
		boolean blacklist = checkBlackList(url);
		if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_FORCE) {
			mActivity.showToast(R.string.black_list_enforced);
		} else if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_WARN) {
			mActivity.askConfirmation(R.string.black_list_warning, new Runnable() {
				@Override
				public void run() {
					save();
					OPDSCatalogEditDialog.super.onPositiveButtonClick();
				}
				
			}, new Runnable() {
				@Override
				public void run() {
					onNegativeButtonClick();
				}
			});
		} else {
			save();
			super.onPositiveButtonClick();
		}
	}
	
	private boolean checkBlackList(String url) {
		for (String s : OPDSConst.BLACK_LIST) {
			if (s.equals(url))
				return true;
		}
		return false;
	}
	
	private void save() {
		activity.getDB().saveOPDSCatalog(mItem.id,
				urlEdit.getText().toString(), nameEdit.getText().toString(), 
				usernameEdit.getText().toString(), passwordEdit.getText().toString());
		mOnUpdate.run();
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		FileInfo file = Services.getScanner().findFileInTree(mItem);
		if (file == null)
			file = mItem;
		if (file.deleteFile()) {
			Services.getHistory().removeBookInfo(mActivity.getDB(), file, true, true);
		}
		super.onThirdButtonClick();
	}

	
}
