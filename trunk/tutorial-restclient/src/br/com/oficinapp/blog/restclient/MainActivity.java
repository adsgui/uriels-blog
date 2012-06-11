/*
 * Copyright 2012 - Oficinapp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.oficinapp.blog.restclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Atividade que irá realizar a pesquisa de estados em um serviço REST,
 * utilizando a biblioteca Apache HttpClient e AsyncTask.
 * 
 * @author Thiago Uriel M. Garcia (oficinapp@gmail.com.br)
 */
public class MainActivity extends ListActivity {
	
	/** Armazenar pois iremos precisar disso! */
	private Context context;
	
	/** Diálogo para tela de espera. */
	private ProgressDialog progressDialog;
	
	/** Lifecycle: Atividade criada. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        context = this;
    	ListView lv = getListView();
    	lv.setTextFilterEnabled(true);
    	lv.setOnItemClickListener(new OnItemClickListener() {
    		
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			Toast.makeText(getApplicationContext(), ((TextView) view).getText(),Toast.LENGTH_SHORT).show();
    	    }
    		
    	});
        
    }

    /** Lifecycle: Atividade reiniciada. */
    @Override
    public void onResume() {
    	
    	//
    	// Irá carregar a lista de estados. Esta tarefa em um cenário real
    	// talvez não deva estar ocorrendo sempre neste evento, mas para
    	// facilitar a exibição, ficará aqui para fins acadêmicos!
    	//
    	super.onResume();
    	new RequestStatesTask().execute("http://192.168.0.107:8080/dummyserver/rest/state");
    	
    }
    
    /**
     * Classe interna que criará um <i>worker</i> responsável por obter estados
     * cadastrados via chamada HTTP, receber o JSON e processá-lo.
     * 
     * @author Thiago Uriel M. Garcia (oficinapp@gmail.com.br)
     */
    private class RequestStatesTask extends AsyncTask<String, Void, String[]> {

    	/** HTTP Client utilizado pela task. */
    	private final HttpClient httpclient = new DefaultHttpClient();
    	
    	/**
    	 * Método chamado antes de iniciar a tarefa. Será executado dentro da
    	 * Thread de Interface.
    	 */
    	protected void onPreExecute() {
    		
    		//
    		// Criar a barra de progresso indeterminado que será exibida
    		// durante a execução do worker thread.
    		//
    		progressDialog = ProgressDialog.show( context
                                                , getString(R.string.progress_title)
                                                , getString(R.string.progress_message)
                                                , true
                                                , true );
    		
    	}
    	
    	/** Tarefa que será realizada por um <i>worker thread</i>. */
		@Override
		protected String[] doInBackground(String... params) {
			
			try {
				
				HttpGet get = new HttpGet(params[0]);
				get.setHeader("Accept", "application/json");
				HttpResponse httpResponse = httpclient.execute(get);
				int receivedStatusCode = httpResponse.getStatusLine().getStatusCode();

				if (receivedStatusCode == HttpStatus.SC_OK) {
					
					//
					// OK, recebemos a lista. Devemos processá-la com os métodos
					// de suporte e devolver o array de strings...
					//
					InputStream inputStream = httpResponse.getEntity().getContent();
					String jsonData = toString(inputStream);
					return handleJson(jsonData);
					
				} else {
					
					//
					// Tivemos algum status diferente de HTTP 200 OK. Retornar
					// nulo e permitir que o método pós-execução notifique.
					//
					return null;
					
				}
				
			} catch (Exception exp) {
				Log.e(RequestStatesTask.class.getName(), "Erro obtendo estados.", exp);
				return null;
			}				
			
		}
		
		/** 
		 * Método chamado após a conclusão da tarefa. Este método será executado
		 * dentro da Thread de Interface e receberá os dados do processamento.
		 */
		protected void onPostExecute(String[] result) {
			
			//
			// Cancelar o medidor de progresso criado anteriormente e mostrar
			// o resultado obtido pela consulta.
			//
			progressDialog.dismiss();
			setListAdapter( new ArrayAdapter<String>( context
					                                , R.layout.state_list_item
					                                , result ) );
			
			//
			// Caso seja nulo, informar que ocorreu algum problema na obtenção
			// dos dados solicitados.
			//
			if (result == null) {

				final AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setCancelable(false)
				       .setTitle(R.string.alert_title)
				       .setMessage(R.string.alert_error)
				       .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
				    	   public void onClick(DialogInterface dialog, int id) {
				    		   return;
				    	   }
				       });

				final AlertDialog alert = builder.create();
				alert.show();
				
			}
			
	    }
    	
		/**
		 * Converte o objeto <code>InputStream</code> recebido pela solicitaçăo
		 * HTTP, e converte-o para um objeto <code>String</code> contendo JSON.
		 * 
		 * @param is 	Objeto <code>InputStream</code> de entrada.
		 * @return 		Objeto <code>String</code> contendo o resultado JSON.
		 */
		private String toString(InputStream is) throws IOException {  

			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO8859-1"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
			    builder.append(line).append("\n");
			}
			return builder.toString();
			
		}
		
		/**
		 * Transforma a String JSON recebida em um array de Strings.
		 * 
		 * @param jsonData	<code>String</code> contendo os dados JSON.
		 * @return			Resultado da conversão dos dados.
		 * 
		 * @throws JSONException
		 * 		Caso ocorra algum problema na conversão do JSON.
		 */
		private String[] handleJson(String jsonData) throws JSONException {
		
			JSONArray jsonArray = new JSONArray(jsonData);
			String[] states = new String[jsonArray.length()];
			
			for (int i = 0; i < jsonArray.length(); i++) {
				
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				states[i] = jsonObject.getString("shortName") + " - " + jsonObject.getString("name");
				
			}
			
			return states;
			
		}
		
    }
    
}