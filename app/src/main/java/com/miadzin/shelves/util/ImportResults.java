/*
 * Copyright (C) 2010 Garen J Torikian
 * Taken liberally from Last.fm Android client
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miadzin.shelves.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.miadzin.shelves.R;

public class ImportResults extends Activity {

	private final String LOG_TAG = "ImportResults";
	private int countImport;
	private int missingImport;
	private String missingItems;
	private int existingImport;
	private String existingItems;
	private int singularID;
	private int pluralID;
	private String resultsFile;

	private Button finishButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.import_results_dialog);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.dialog_title);

		final TextView importTitle = (TextView) findViewById(R.id.dialogTitle);
		if (importTitle != null) {
			importTitle.setText(getString(R.string.import_results));
		}

		countImport = this.getIntent().getExtras().getInt("countImport");
		missingImport = this.getIntent().getExtras().getInt("missingImport");
		missingItems = this.getIntent().getExtras().getString("missingItems");
		existingImport = this.getIntent().getExtras().getInt("existsImport");
		existingItems = this.getIntent().getExtras().getString("existingItems");
		singularID = this.getIntent().getExtras().getInt("singularID");
		pluralID = this.getIntent().getExtras().getInt("pluralID");
		resultsFile = this.getIntent().getExtras().getString("resultsFile");

		StringBuilder successResults = new StringBuilder();
		if (countImport >= 1) {
			TextView success = (TextView) findViewById(R.id.successfulImportResults);
			success.setVisibility(View.VISIBLE);
			if (countImport == 1)
				successResults.append(getString(R.string.success_imported_item,
						countImport, getString(singularID)));
			else
				successResults.append(getString(R.string.success_imported_item,
						countImport, getString(pluralID)));
			success.setText(successResults.toString());
		}

		StringBuilder missingResults = new StringBuilder();
		if (missingImport >= 1) {
			TextView resultDescription = (TextView) findViewById(R.id.importResultsDescription);
			resultDescription.setVisibility(View.VISIBLE);
			resultDescription.setText(getString(
					R.string.import_result_description,
					resultsFile.substring(resultsFile.indexOf("/") + 1)));

			TextView missing = (TextView) findViewById(R.id.missingImportResults);
			missing.setVisibility(View.VISIBLE);

			if (missingImport == 1)
				missingResults.append(getString(
						R.string.import_missing_items_singular, missingImport,
						getString(singularID)));
			else if (missingImport < 25)
				missingResults.append(getString(
						R.string.import_missing_items_plural, missingImport,
						getString(pluralID)));
			else
				missingResults.append(getString(
						R.string.import_missing_items_limit, missingImport,
						getString(pluralID)));

			missingResults.append(missingItems);

			missing.setText(missingResults.toString());
		}

		StringBuilder existingResults = new StringBuilder();
		if (existingImport >= 1) {
			TextView existing = (TextView) findViewById(R.id.existingImportResults);
			existing.setVisibility(View.VISIBLE);

			if (existingImport == 1)
				existingResults.append(getString(
						R.string.import_existing_items_singular,
						existingImport, getString(singularID)));
			else if (existingImport < 25)
				existingResults.append(getString(
						R.string.import_existing_items_plural, existingImport,
						getString(pluralID)));
			else
				existingResults.append(getString(
						R.string.import_existing_items_limit, existingImport,
						getString(pluralID)));

			existingResults.append(existingItems);

			existing.setText(existingResults.toString());
		}

		this.deleteFile(IOUtilities.getFileName(resultsFile));
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(IOUtilities.getExternalFile(resultsFile));

			int leftParenMissingPos = missingResults.indexOf("(");
			int leftParenExistingPos = existingResults.indexOf("(");

			if (leftParenMissingPos > 0)
				fos.write(missingResults
						.delete(leftParenMissingPos,
								missingResults.indexOf(")")).toString()
						.getBytes());
			else
				fos.write(missingResults.toString().getBytes());

			if (leftParenExistingPos > 0)
				fos.write(existingResults
						.delete(leftParenExistingPos,
								existingResults.indexOf(")")).toString()
						.getBytes());
			else
				fos.write(existingResults.toString().getBytes());

			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		finishButton = (Button) this.findViewById(R.id.loan_results_finish);
		finishButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}
}
