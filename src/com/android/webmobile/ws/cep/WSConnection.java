package com.android.webmobile.ws.cep;

import java.io.IOException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

public class WSConnection {
	private static final String URL = "http://www.maniezo.com.br/webservice/soap-server.php";
	private static final String OPERATION = "traz_cep";
	private static final String NAMESPACE = "http://www.maniezo.com.br/";
	private static final String USERNAME = "leandro.hossaki";
	private static final String PASSWORD = "LeandrO";
	public static Object pesquisarCEP(String cep) {
		SoapObject request = new SoapObject(NAMESPACE, OPERATION);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		StringBuffer params = new StringBuffer();
		params.append(cep.concat("#"));
		params.append(USERNAME.concat("#"));
		params.append(PASSWORD.concat("#"));
		request.addProperty("dados_cep", params.toString());
		HttpTransportSE httpTransport = new HttpTransportSE(URL);
		try {
			httpTransport.call("", envelope);
			String response = (String) envelope.getResponse();
			if (response.equals("#####"))
				return null;
			else {
				String[] dadosCep = response.split("#");
				CepBean cepBean = new CepBean();
				cepBean.setLogradouro(dadosCep[0] + " " + dadosCep[1]);
				cepBean.setComplemento(dadosCep[2]);
				cepBean.setBairro(dadosCep[3]);
				cepBean.setCidade(dadosCep[4]);
				cepBean.setUf(dadosCep[5]);
				cepBean.setCep(cep);
				return cepBean;
			}
		}
		catch (IOException ioex) {
			return null;
		}
		catch (XmlPullParserException e) {
			return null;
		}
	}
}
