package com.najunho.datecourserecommendapplication.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.najunho.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.najunho.datecourserecommendapplication.R;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingActivity extends AppCompatActivity {
    private CloudFunctionManager cloudFunctionManager;
    private BillingClient billingClient;

    // productId → ProductDetails 매핑
    private Map<String, ProductDetails> productDetailsMap = new HashMap<>();

    // 네가 만든 인앱 상품 ID들
    private static final String PRODUCT_500 = "point_500";
    private static final String PRODUCT_2000 = "point_2000";
    private static final String PRODUCT_5000 = "point_5000";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing); // Make sure you have this layout file

        cloudFunctionManager = new CloudFunctionManager();

        // 1. BillingClient 초기화
        initBillingClient();

        // 2. UI 버튼에 클릭 리스너 설정
        Button buy500Button = findViewById(R.id.buy_500_button); // Assume you have these buttons in your XML
        Button buy2000Button = findViewById(R.id.buy_2000_button);
        Button buy5000Button = findViewById(R.id.buy_5000_button);
        ImageButton backBtn = findViewById(R.id.btnBack);

        buy500Button.setOnClickListener(v -> launchPurchaseFlow(PRODUCT_500));
        buy2000Button.setOnClickListener(v -> launchPurchaseFlow(PRODUCT_2000));
        buy5000Button.setOnClickListener(v -> launchPurchaseFlow(PRODUCT_5000));
        backBtn.setOnClickListener(v->{
            finish();
        });
    }

    // ✅ STEP 1: 결제 결과(성공, 실패, 취소)를 수신하는 리스너 정의 : billingClient.launchBillingFlow() -> 상호작용 결과 정의
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                    // 구매 성공 → 여기서 아이템 지급 처리
                    Log.d("purchasesUpdatedListener", "Purchase is completed.");
                    handlePurchase(purchase);
                }else {
                    Log.d("purchasesUpdatedListener", "Purchase not complete.");
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // 사용자가 구매 취소
            Log.d("purchasesUpdatedListener", "User canceled the purchase.");
            Toast.makeText(this, "Purchase canceled.", Toast.LENGTH_SHORT).show();
        } else {
            // 기타 에러
            Log.e("purchasesUpdatedListener", "Purchase error: " + billingResult.getDebugMessage());
            Toast.makeText(this, "Error: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts() // 일회성 제품 대기 결제 허용 (필수)
                        // 만약 정기 결제도 있다면 .enablePrepaidPlans() 추가 가능
                        .build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("initBillingClient", "BillingClient is ready.");
                    // Billing 준비 완료 → 상품 정보 요청
                    queryProductDetails();
                } else {
                    Log.e("initBillingClient", "BillingClient setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BILLING", "BillingClient disconnected. Trying to reconnect...");
                // 네트워크 끊김 등으로 연결 해제됨 -> 재시도 로직
                // initBillingClient(); // or a more robust retry policy
            }
        });
    }

    // ✅ STEP 2: Google Play에 등록된 상품 정보를 가져오는 메소드 구현
    private void queryProductDetails() {
        // 쿼리할 상품 목록 생성
        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_500)
                        .setProductType(BillingClient.ProductType.INAPP) // 소모성 상품은 INAPP
                        .build(),
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_2000)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_5000)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        // 쿼리 파라미터 생성
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        // 상품 정보 비동기 요청
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                // productDetailsMap에 결과 저장
                productDetailsMap.clear();
                for (ProductDetails productDetails : productDetailsList) {
                    productDetailsMap.put(productDetails.getProductId(), productDetails);
                    Log.d("queryProductDetailsAsync", "Found product: " + productDetails.getName() + ", Price: " + productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
                }
                // 이제 구매 버튼을 활성화하거나 UI를 업데이트할 수 있습니다.
            } else {
                Log.e("queryProductDetailsAsync", "Failed to query product details: " + billingResult.getDebugMessage());
            }
        });
    }

    // ✅ STEP 3: 구매 흐름을 시작하는 메소드 구현
    public void launchPurchaseFlow(String productId) {
        ProductDetails productDetails = productDetailsMap.get(productId);

        if (productDetails == null) {
            Toast.makeText(this, "Product not available.", Toast.LENGTH_SHORT).show();
            Log.e("BILLING", "Cannot launch purchase flow because ProductDetails is null for " + productId);
            return;
        }

        // 구매 흐름 파라미터 생성
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        Log.d("BILLING", "start launchBillingFlow");
        // 구매 흐름 시작 -> billingClient.launchBillingFlow : 결제 팝업 띄움
        BillingResult billingResult = billingClient.launchBillingFlow(this, billingFlowParams);
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e("BILLING", "Failed to launch billing flow: " + billingResult.getDebugMessage());
        }
    }

    // ✅ STEP 4: 구매 성공 시 처리 로직
    private void handlePurchase(Purchase purchase) {
        // TODO: 여기서 실제로 사용자에게 아이템을 지급하는 로직을 구현합니다.
        // 예를 들어, Firebase DB에 사용자의 포인트 정보를 업데이트합니다.

        // 1. purchaseToken 얻기
        String purchaseToken = purchase.getPurchaseToken();

        // 2. productId 추출 (안전하게)
        List<String> products = purchase.getProducts();
        String productId = "";

        if (products != null && !products.isEmpty()) {
            productId = products.get(0); // 사용자가 클릭한 그 상품 ID
        }

        // 3. orderId 얻기 (GPA.XXXX-XXXX-XXXX-XXXX 형태)
        String orderId = purchase.getOrderId();

        if (!productId.isEmpty() && orderId != null) {
            // 백엔드(Firebase Functions) 호출
            cloudFunctionManager.callVerifyPurchase(purchaseToken, productId, orderId, new CloudFunctionManager.PointCallback() {
                    @Override
                    public void onSuccess(boolean success, String result) {
                        consumeItem(purchase);
                        Log.d("callVerifyPurchase", "Purchase successful for: " + purchase.getProducts().get(0));
                        Toast.makeText(BillingActivity.this, "Purchase successful!", Toast.LENGTH_LONG).show();
                    }

                @Override
                public void onError(Exception e) {
                    Log.d("callVerifyPurchase", "error: "+ e);
                }
            });
        } else {
            Log.e("callVerifyPurchase", "결제 정보 추출 실패: 상품 ID 혹은 주문 번호가 없습니다.");
        }
    }

    private void consumeItem(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, (billingResult, outToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d("consumeItem", "Item consumed successfully.");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
