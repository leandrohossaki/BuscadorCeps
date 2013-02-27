package com.android.webmobile.ws.cep;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard.Key;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class WSCepActivity extends MapActivity implements OnClickListener, OnKeyListener, Runnable {
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	
	private ViewFlipper flipper;
	
	private EditText etCEPDe;
	private EditText etCEPPara;
	private ImageButton imgBtnPesquisar;
	
	private TextView tvDeEstado;
	private TextView tvDeCidade;
	private TextView tvDeBairro;
	private TextView tvDeLogradouro;
	private TextView tvDeComplemento;
	
	private TextView tvParaEstado;
	private TextView tvParaCidade;
	private TextView tvParaBairro;
	private TextView tvParaLogradouro;
	private TextView tvParaComplemento;
	
	private MapView mapView;
	
	private ProgressDialog progressDialog;
	private AlertDialog alertDialog;
	
	private CepBean cepBeanDe;
	private CepBean cepBeanPara;
	
	private Road road;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		flipper = (ViewFlipper)findViewById(R.id.flipper);
		Animation in = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		
		flipper.setInAnimation(in);
		flipper.setOutAnimation(out);
		flipper.startFlipping();
		
		etCEPDe = (EditText)findViewById(R.id.etCEPDe);
		etCEPPara = (EditText)findViewById(R.id.etCEPPara);
		imgBtnPesquisar = (ImageButton)findViewById(R.id.imgBtnPesquisar);
		
		tvDeEstado = (TextView)findViewById(R.id.tvDeEstado);
		tvDeCidade = (TextView)findViewById(R.id.tvDeCidade);
		tvDeBairro = (TextView)findViewById(R.id.tvDeBairro);
		tvDeLogradouro = (TextView)findViewById(R.id.tvDeLogradouro);
		tvDeComplemento = (TextView)findViewById(R.id.tvDeComplemento);
		
		tvParaEstado = (TextView)findViewById(R.id.tvParaEstado);
		tvParaCidade = (TextView)findViewById(R.id.tvParaCidade);
		tvParaBairro = (TextView)findViewById(R.id.tvParaBairro);
		tvParaLogradouro = (TextView)findViewById(R.id.tvParaLogradouro);
		tvParaComplemento = (TextView)findViewById(R.id.tvParaComplemento);

		mapView = (MapView)findViewById(R.id.map);
		mapView.setBuiltInZoomControls(true);
		
		//LinearLayout zoomLayout = (LinearLayout)findViewById(R.id.map_zoom);
		//zoomLayout.addView(mapView.getZoomControls(), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		imgBtnPesquisar.setOnClickListener(this);
		etCEPDe.setOnKeyListener(this);
		etCEPPara.setOnKeyListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu subMenu = menu.addSubMenu(0,1,2,"Modo de Mapa").setIcon(android.R.drawable.ic_menu_mapmode);
		subMenu.add(0,2,1,"Tráfego");
		subMenu.add(0,3,2,"Satélite");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case 2:
				mapView.setTraffic(true);
				mapView.setSatellite(false);
				break;
			case 3:
				mapView.setTraffic(false);
				mapView.setSatellite(true);
				break;
		}
		return true;
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
			pesquisarCEP();
            return true;
        }
		return false;
	}

	@Override
	public void onClick(View v) {
		pesquisarCEP();
	}
	
	private void pesquisarCEP(){
		progressDialog = ProgressDialog.show(this, "Buscando CEP", "Por Favor, Aguarde...", true, false);
		Thread thread = new Thread(this);
		thread.start();
	}
	
	private InputStream getConnection(String url) {
        InputStream is = null;
        try {
                URLConnection conn = new URL(url).openConnection();
                is = conn.getInputStream();
        } catch (MalformedURLException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        }
        return is;
	}

	private Route directions(final GeoPoint start, final GeoPoint dest) {
	    Parser parser;
	    //https://developers.google.com/maps/documentation/directions/#JSON <- get api
	    String jsonURL = "http://maps.googleapis.com/maps/api/directions/json?";
	    final StringBuffer sBuf = new StringBuffer(jsonURL);
	    sBuf.append("origin=");
	    sBuf.append(start.getLatitudeE6()/1E6);
	    sBuf.append(',');
	    sBuf.append(start.getLongitudeE6()/1E6);
	    sBuf.append("&destination=");
	    sBuf.append(dest.getLatitudeE6()/1E6);
	    sBuf.append(',');
	    sBuf.append(dest.getLongitudeE6()/1E6);
	    sBuf.append("&sensor=true&mode=driving");
	    parser = new GoogleParser(sBuf.toString());
	    Route r =  parser.parse();
	    return r;
	}
	
	@Override
	public void run() {
		if(etCEPDe.getText().toString().equals("")) {
			Bundle bundle = new Bundle();
			bundle.putString("titulo", "CEP de partida em Branco");
			bundle.putString("msg", "Por Favor informe o CEP de partida");
			Message m = Message.obtain();
			m.setData(bundle);
			handlerError.sendMessage(m);
		} else if(etCEPPara.getText().toString().equals("")) {
			Bundle bundle = new Bundle();
			bundle.putString("titulo", "CEP de chegada em Branco");
			bundle.putString("msg", "Por Favor informe o CEP de chegada");
			Message m = Message.obtain();
			m.setData(bundle);
			handlerError.sendMessage(m);
		} else {
			cepBeanDe = (CepBean)WSConnection.pesquisarCEP(etCEPDe.getText().toString().trim());
			cepBeanPara = (CepBean)WSConnection.pesquisarCEP(etCEPPara.getText().toString().trim());
			
			if(cepBeanDe != null && cepBeanPara != null) {
				try {
					Geocoder geocoder = new Geocoder(this);
					String localDe = cepBeanDe.getLogradouro() + ", " + cepBeanDe.getCidade();
					Address localidadeDe = geocoder.getFromLocationName(localDe, 1).get(0);
					String localPara = cepBeanPara.getLogradouro() + ", " + cepBeanPara.getCidade();
					Address localidadePara = geocoder.getFromLocationName(localPara, 1).get(0);
					if (localidadeDe != null && localidadePara != null) {
						double latitudeDe = localidadeDe.getLatitude() * 1E6;
						double longitudeDe = localidadeDe.getLongitude() * 1E6;
						GeoPoint geoPointDe = new GeoPoint((int)latitudeDe, (int)longitudeDe);
						
						double latitudePara = localidadePara.getLatitude() * 1E6;
						double longitudePara = localidadePara.getLongitude() * 1E6;
						GeoPoint geoPointPara = new GeoPoint((int)latitudePara, (int)longitudePara);
						
						// zooming to both points
						int maxLatitude = Math.max((int)latitudeDe, (int)latitudePara);
						int minLatitude = Math.min((int)latitudeDe, (int)latitudePara);
						int maxLongitude = Math.max((int)longitudeDe, (int)longitudePara);
						int minLongitude = Math.min((int)longitudeDe, (int)longitudePara);
						
						MapController mapController = mapView.getController();
						mapController.zoomToSpan(maxLatitude - minLatitude, maxLongitude - minLongitude);
						mapController.animateTo(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
						
						/*mapController.setZoom(17);
						mapController.animateTo(geoPointDe);*/
						
						Drawable marker = getResources().getDrawable(R.drawable.pin);
						if (mapView.getOverlays().size() > 0)
							mapView.getOverlays().clear();
						
						LocationCepOverlay locationCepOverlay = new LocationCepOverlay(marker, this);
						
						OverlayItem item = new OverlayItem(geoPointDe, "Partida", cepBeanDe.getLogradouro());
						locationCepOverlay.add(item);
						
						OverlayItem item2 = new OverlayItem(geoPointPara, "Chegada", cepBeanPara.getLogradouro());
						locationCepOverlay.add(item2);
						
						mapView.getOverlays().add(locationCepOverlay);
						
						/* ========================================================= */
						Route route = directions(geoPointDe, geoPointPara);
						RouteOverlay routeOverlay = new RouteOverlay(route, Color.BLUE);
						mapView.getOverlays().add(routeOverlay);
						//mapView.invalidate();						
						
						/*String url = RoadProvider.getUrl(localidadeDe.getLatitude(), localidadeDe.getLongitude(), localidadePara.getLatitude(), localidadePara.getLongitude());
		                InputStream is = getConnection(url);
		                road = RoadProvider.getRoute(is);

		                MapOverlay mapOverlay = new MapOverlay(road, mapView);
		                mapView.getOverlays().add(mapOverlay);*/
		                /* ========================================================= */
						
						handler.sendEmptyMessage(0);
					}
					else {
						Bundle bundle = new Bundle();
						bundle.putString("titulo", "Localidade não encontrada");
						bundle.putString("msg","Não foi possível encontrar uma localidade no mapa para este cep.");
						Message m = Message.obtain();
						m.setData(bundle);
						handlerError.sendMessage(m);
					}
				} catch (IOException ex) {
					Bundle bundle = new Bundle();
					bundle.putString("titulo", "Erro de IO");
					bundle.putString("msg", "Erro ao obter localidade. Mensagem: " + ex.getMessage());
					Message m = Message.obtain();
					m.setData(bundle);
					handlerError.sendMessage(m);
				} catch (Exception ex) {
					Bundle bundle = new Bundle();
					bundle.putString("titulo", "Erro Geral");
					bundle.putString("msg", "Ocorreu um erro geral. Mensagem: "
					+ ex.getMessage());
					Message m = Message.obtain();
					m.setData(bundle);
					handlerError.sendMessage(m);
				}
			} else {
				Bundle bundle = new Bundle();
				bundle.putString("titulo", "CEP não encontrado");
				bundle.putString("msg","Desculpe, o CEP não pôde ser encontrado. Tente novamente.");
				Message m = Message.obtain();
				m.setData(bundle);
				handlerError.sendMessage(m);
			}
		}
	}
	
	//Handler para atualizar os dados da localidade do cep
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			tvDeEstado.setText(cepBeanDe.getUf());
			tvDeCidade.setText(cepBeanDe.getCidade());
			tvDeBairro.setText(cepBeanDe.getBairro());
			tvDeLogradouro.setText(cepBeanDe.getLogradouro());
			tvDeComplemento.setText(cepBeanDe.getComplemento());
			
			tvParaEstado.setText(cepBeanPara.getUf());
			tvParaCidade.setText(cepBeanPara.getCidade());
			tvParaBairro.setText(cepBeanPara.getBairro());
			tvParaLogradouro.setText(cepBeanPara.getLogradouro());
			tvParaComplemento.setText(cepBeanPara.getComplemento());
			
			progressDialog.dismiss();
		}
	};
	
	// Handler para mostrar mensagens de erro
	private Handler handlerError = new Handler() {
		public void handleMessage(Message msg) {
			if (progressDialog.isShowing()) 
				progressDialog.dismiss();
			
			tvDeEstado.setText(getString(R.string.de_estado));
			tvDeCidade.setText(getString(R.string.de_cidade));
			tvDeBairro.setText(getString(R.string.de_bairro));
			tvDeLogradouro.setText(getString(R.string.de_logradouro));
			tvDeComplemento.setText(getString(R.string.de_complemento));
			
			tvParaEstado.setText(getString(R.string.de_estado));
			tvParaCidade.setText(getString(R.string.de_cidade));
			tvParaBairro.setText(getString(R.string.de_bairro));
			tvParaLogradouro.setText(getString(R.string.de_logradouro));
			tvParaComplemento.setText(getString(R.string.de_complemento));
			
			String titulo = msg.getData().getString("titulo");
			String mensagem = msg.getData().getString("msg");
			
			alertDialog = new AlertDialog.Builder(WSCepActivity.this).create();
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					alertDialog.dismiss();
				}
			});
			alertDialog.setTitle(titulo);
			alertDialog.setMessage(mensagem);
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.show();
		}
	};
}

class LocationCepOverlay extends ItemizedOverlay<OverlayItem> {
	private List<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	private Drawable marker;
	private Context context;
	public LocationCepOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.marker = defaultMarker;
		this.context = context;
	}
	@Override
	protected OverlayItem createItem(int i) {
		return overlayItems.get(i);
	}
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		boundCenterBottom(marker);
	}
	@Override
	public int size() {
		return overlayItems.size();
	}
	@Override
	protected boolean onTap(int index){
		OverlayItem item = overlayItems.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}
	public void add(OverlayItem overlay){
		overlayItems.add(overlay);
		populate();
	}
}

class MapOverlay extends com.google.android.maps.Overlay {
    Road mRoad;
    ArrayList<GeoPoint> mPoints;

    public MapOverlay(Road road, MapView mv) {
        mRoad = road;
        if (road.mRoute.length > 0) {
            mPoints = new ArrayList<GeoPoint>();
            for (int i = 0; i < road.mRoute.length; i++) {
                mPoints.add(new GeoPoint((int) (road.mRoute[i][1] * 1000000), (int) (road.mRoute[i][0] * 1000000)));
            }
            int moveToLat = (mPoints.get(0).getLatitudeE6() + (mPoints.get(mPoints.size() - 1).getLatitudeE6() - mPoints.get(0).getLatitudeE6()) / 2);
            int moveToLong = (mPoints.get(0).getLongitudeE6() + (mPoints.get(mPoints.size() - 1).getLongitudeE6() - mPoints.get(0).getLongitudeE6()) / 2);
            GeoPoint moveTo = new GeoPoint(moveToLat, moveToLong);

            MapController mapController = mv.getController();
            mapController.animateTo(moveTo);
            mapController.setZoom(7);
        }
    }

    @Override
    public boolean draw(Canvas canvas, MapView mv, boolean shadow, long when) {
        super.draw(canvas, mv, shadow);
        drawPath(mv, canvas);
        return true;
    }

    public void drawPath(MapView mv, Canvas canvas) {
        int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        if(mPoints != null) {
	        for (int i = 0; i < mPoints.size(); i++) {
	            Point point = new Point();
	            mv.getProjection().toPixels(mPoints.get(i), point);
	            x2 = point.x;
	            y2 = point.y;
	            if (i > 0) {
	                canvas.drawLine(x1, y1, x2, y2, paint);
	            }
	            x1 = x2;
	            y1 = y2;
	        }
        }
    }
}
