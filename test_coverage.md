# Hướng dẫn rà soát test suite và coverage (JaCoCo) cho YAS

## 1. Mục tiêu tài liệu

Tài liệu này dùng để:
- Rà soát toàn bộ test suite theo hướng hiệu quả và phản ánh đúng business logic.
- Tách biệt business service và infrastructure service để tránh đánh giá coverage sai phạm vi.
- Chuẩn hóa cách chạy test và đọc report JaCoCo.
- Đề xuất cách viết unit test cho các business service có **branch coverage < 70%**.

Phạm vi tài liệu:
- Chỉ tập trung **business service đang active** trong `docker-compose.yml`.
- Không bao gồm snapshot số liệu coverage cố định; tài liệu cung cấp quy trình để tự đo và kiểm chứng.

---

## 2. Phân loại service trong codebase

### 2.1 Business service (active)

Dựa trên `docker-compose.yml` và module Maven trong `pom.xml`, các business service đang active:
- `product` (xong - 67%)
- `media` (xong - 57%)
- `customer` (xong - 88%)
- `cart` (xong - 91%)
- `rating`
- `order` (xong - 76%)
- `payment` (xong - 62%)
- `location`
- `inventory` (xong - 91%)
- `promotion`
- `tax` (xong - 70%)
- `search`

| service | Missed Instruction | Missed Branch |
|---|--------------------|--------------:|
| product | 67%                |           39% | 
| media | 57%                |           43% | 
| customer | 88%                |           87% |
| cart | 91%                |           68% |
| rating |                    |               | 
| order | 76%                |           48% |  
| payment | 62%                |           29% |   
| location |                    |               |   
| inventory | 91%                |           81% |   
| promotion |                    |               |   
| tax | 70%                |           93% |   

Tiêu chí xếp business service:
- Có domain nghiệp vụ riêng (catalog, order, payment, inventory, ...).
- Có API phục vụ use case nghiệp vụ.
- Có persistence/schema riêng hoặc logic nghiệp vụ rõ ràng.

### 2.2 Infrastructure / cross-cutting service

Các thành phần hạ tầng hoặc hỗ trợ vận hành (không đưa vào KPI coverage business):
- API/BFF/Gateway: `storefront-bff`, `backoffice-bff`, `nginx`
- Identity/Auth: `identity` (Keycloak)
- Data/Message/Cache: `postgres`, `kafka`, `kafka-connect`, `zookeeper`, `redis`, `kafka-ui`, `pgadmin`
- Observability: stack trong `docker-compose.o11y.yml` (`otel-collector`, `prometheus`, `grafana`, `loki`, `tempo`)
- Tooling/Shared: `common-library`, `sampledata`, `payment-paypal`, `swagger-ui`
- Service đang comment/chưa active trong compose chính: `webhook`, `recommendation` (không thuộc phạm vi active hiện tại)

Lưu ý:
- `search` được xem là business service vì cung cấp chức năng nghiệp vụ tìm kiếm sản phẩm cho hệ thống.

---

## 3. Hiện trạng test suite và JaCoCo

### 3.1 Cấu trúc test

Chuẩn đang dùng trong repo:
- Unit test: `src/test/java` với tên lớp `*Test.java`
- Integration test: `src/it/java` với tên lớp `*IT.java`
- Integration resources: `src/it/resources`

### 3.2 Cấu hình chạy test/coverage ở Maven

Trong `pom.xml` root:
- `jacoco-maven-plugin`:
  - `prepare-agent`
  - `report` gắn vào phase `verify`
- `maven-failsafe-plugin`:
  - chạy integration test với include `**/**IT.java`
- `build-helper-maven-plugin`:
  - add source `src/it/java` và resource `src/it/resources`

Ý nghĩa thực tế:
- `mvn clean test ...` chủ yếu phục vụ vòng lặp unit test nhanh.
- `mvn clean install ...` hoặc `mvn clean verify ...` sẽ chạy đầy đủ hơn (kèm failsafe + JaCoCo report ở phase `verify`).

---

## 4. Cách chạy test và sinh report coverage

### 4.1 Chuẩn bị

Chạy tại thư mục gốc repo `yas`.

PowerShell:

```powershell
Set-Location "f:\Dai hoc\Year 4\HK2\NMDevOps\Project1\yas"
```

### 4.2 Chạy test nhanh cho 1 business service (ưu tiên local loop)

```powershell
mvn clean test -pl product -am
```

Thay `product` bằng service cần kiểm tra: `media`, `customer`, `cart`, `rating`, `order`, `payment`, `location`, `inventory`, `promotion`, `tax`, `search`.

### 4.3 Chạy đầy đủ để có report JaCoCo

```powershell
mvn clean install -pl product -am
```

Sau khi chạy xong, report nằm tại:
- HTML: `product/target/site/jacoco/index.html`
- XML: `product/target/site/jacoco/jacoco.xml`

### 4.4 Chạy tuần tự toàn bộ business service active

```powershell
$services = @(
  "product","media","customer","cart","rating","order",
  "payment","location","inventory","promotion","tax","search"
)

foreach ($s in $services) {
  Write-Host "===== $s =====" -ForegroundColor Cyan
  mvn clean install -pl $s -am
}
```

### 4.5 Đường dẫn report cần kiểm tra sau mỗi service

Mẫu:
- `{service}/target/site/jacoco/index.html`
- `{service}/target/site/jacoco/jacoco.xml`
- `{service}/target/surefire-reports/`
- `{service}/target/failsafe-reports/`

---

## 5. Cách đọc JaCoCo theo ngưỡng branch < 70%

Khi mở `index.html` của JaCoCo:
- Ưu tiên cột `Branches` (C1) hơn chỉ nhìn `Lines`.
- Xác định package/class có branch thấp.
- Drill-down đến class/method để xem nhánh nào missed.

Ưu tiên xử lý theo mức độ rủi ro nghiệp vụ:
1. `order`, `payment`, `cart`, `tax`, `inventory`
2. `product`, `promotion`, `customer`, `rating`
3. `media`, `location`, `search`

Nguyên tắc đánh giá:
- Branch coverage < 70% ở lớp nghiệp vụ quan trọng cần mở ticket cải thiện ngay.
- Không chạy theo số coverage tổng; phải gắn với đường đi nghiệp vụ cốt lõi.

---

## 6. Checklist rà soát chất lượng test suite business

Checklist cho từng business service:
- Có test cho luồng thành công (happy path) chính.
- Có test cho điều kiện biên (boundary) và dữ liệu bất thường.
- Có test cho validation rule theo domain.
- Có test cho luồng lỗi nghiệp vụ (business exception).
- Có test cho nhánh rẽ điều kiện (if/else/switch) ở service layer.
- Có test cho side effects quan trọng (gọi repository, event, external client).
- Test độc lập, deterministic, không phụ thuộc thứ tự chạy.
- Tên test mô tả rõ rule nghiệp vụ.

Dấu hiệu test kém hiệu quả:
- Test chỉ verify getter/setter hoặc DTO mapping đơn giản.
- Mock quá nhiều dẫn đến không còn kiểm chứng rule business thực sự.
- Dùng dữ liệu test không phản ánh domain thật.
- Branch coverage thấp ở class nghiệp vụ nhưng cao ở controller/repository test.

---

## 7. Chiến lược viết unit test cho service có branch coverage < 70%

### 7.1 Ưu tiên theo business rule

Đối với mỗi service dưới ngưỡng:
1. Liệt kê các method nghiệp vụ chính.
2. Với từng method, liệt kê toàn bộ nhánh điều kiện.
3. Map từng nhánh sang ít nhất 1 test case.
4. Bổ sung case lỗi/exception trước khi thêm case phụ.

### 7.2 Mẫu test case nên có

- Happy path:
  - Dữ liệu hợp lệ, trạng thái hợp lệ, kết quả thành công.
- Guard/validation:
  - Thiếu dữ liệu, dữ liệu sai format, vi phạm rule.
- State transition:
  - Chuyển trạng thái hợp lệ/không hợp lệ (đặc biệt cho order/payment).
- External dependency failure:
  - Lỗi từ repository/rest client/message broker được xử lý đúng.
- Authorization/ownership (nếu có trong business layer):
  - User không có quyền hoặc không sở hữu tài nguyên.

### 7.3 Kỹ thuật triển khai unit test

- Dùng JUnit 5 + Mockito.
- Tạo fixture theo domain (builder/factory) để đọc test dễ.
- Chỉ mock dependency ngoài unit cần test.
- Verify cả output và interaction quan trọng.
- Với logic có nhiều nhánh, dùng parameterized test để tránh trùng lặp.

Ví dụ skeleton:

```java
@DisplayName("OrderService - placeOrder")
class OrderServiceTest {

  @Test
  void should_place_order_successfully_when_cart_valid_and_stock_available() {
    // arrange
    // act
    // assert
  }

  @Test
  void should_reject_when_cart_is_empty() {
    // arrange
    // act + assert exception
  }

  @Test
  void should_reject_when_payment_not_authorized() {
    // arrange
    // act + assert exception
  }
}
```

---

## 8. Mẫu báo cáo coverage để team tự điền

Sử dụng bảng sau cho mỗi đợt rà soát:

| Service | Branch Coverage (%) | Trạng thái | Class/Method rủi ro cao | Kế hoạch bổ sung test | Owner | ETA |
|---|---:|---|---|---|---|---|
| order |  | <70 / >=70 |  |  |  |  |
| payment |  | <70 / >=70 |  |  |  |  |
| cart |  | <70 / >=70 |  |  |  |  |
| tax |  | <70 / >=70 |  |  |  |  |
| inventory |  | <70 / >=70 |  |  |  |  |
| product |  | <70 / >=70 |  |  |  |  |
| promotion |  | <70 / >=70 |  |  |  |  |
| customer |  | <70 / >=70 |  |  |  |  |
| rating |  | <70 / >=70 |  |  |  |  |
| media |  | <70 / >=70 |  |  |  |  |
| location |  | <70 / >=70 |  |  |  |  |
| search |  | <70 / >=70 |  |  |  |  |

Quy tắc đánh dấu:
- `<70`: bắt buộc có action plan cụ thể.
- `>=70`: vẫn cần rà soát chất lượng test, không chỉ nhìn con số.

---

## 9. Definition of Done cho cải thiện coverage business

Một business service được coi là đạt khi:
- Branch coverage đạt tối thiểu 70%.
- Các luồng nghiệp vụ cốt lõi đã có test (happy + validation + error path).
- Test chạy ổn định trên local và CI.
- Không thêm flaky test.
- PR có mô tả rõ phần business rule đã được kiểm chứng.

---

## 10. Tham chiếu nhanh

Các file tham chiếu chính:
- `pom.xml` (module list, JaCoCo, failsafe, build-helper)
- `docker-compose.yml` (service active/inactive)
- `.github/workflows/*-ci.yaml` (cách CI chạy test và đọc JaCoCo)
- `docs/developer-guidelines.md` (nguyên tắc tách business logic vào service)

Gợi ý quy trình chuẩn cho mỗi service:
1. Chạy `mvn clean install -pl <service> -am`
2. Mở `target/site/jacoco/index.html`
3. Xác định class branch < 70%
4. Viết bổ sung unit test theo checklist mục 6-7
5. Chạy lại và cập nhật bảng mục 8