# Backend Test Case Catalog — Yard Management System

## 1. Mục đích và phạm vi

Catalog này là danh mục test-case khởi tạo cho 10 domain: User, Confirmation, Partner, Contract, Service, Vehicle, Yard, Goods, Tracking và Finance. Catalog hỗ trợ AI/dev chọn và mở rộng test cho từng thay đổi; không thay thế acceptance criteria đã được phê duyệt.

**Quy tắc:** Expected result dưới đây mô tả nguyên tắc quan sát được. HTTP status, error code, field limit, quyền, trạng thái và quy tắc nghiệp vụ cụ thể phải lấy từ implementation/spec thực tế. Không tự suy ra giá trị chưa được xác nhận.

## 2. Quy ước test-case

| Field | Nội dung |
|---|---|
| Test Case ID | Mã duy nhất, ví dụ USER-001 |
| Requirement ID | AC/ticket/spec được kiểm chứng |
| Domain/Feature | Domain và chức năng |
| Priority | P0 critical, P1 high, P2 normal, P3 low |
| Type | Positive, Negative, Boundary, Security, Integration, Regression |
| Preconditions | Dữ liệu, trạng thái, actor |
| Test data | Input cụ thể, không dùng dữ liệu production |
| Steps | Các bước thực hiện |
| Expected result | Kết quả quan sát được, trạng thái dữ liệu và side effect |
| Automation | Tên test class/method hoặc Manual |
| Result | PASS / FAIL / BLOCKED / NOT RUN |

### Quy tắc ID

- `USER-xxx`, `CONF-xxx`, `PART-xxx`, `CONT-xxx`, `SERV-xxx`
- `VEH-xxx`, `YARD-xxx`, `GOOD-xxx`, `TRK-xxx`, `FIN-xxx`
- `SEC-xxx`, `API-xxx`, `DB-xxx` cho test cross-domain.

Không tái sử dụng ID đã tồn tại. Khi thay đổi yêu cầu, cập nhật traceability thay vì âm thầm xóa test cũ.

## 3. Test-case dùng chung

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| API-001 | P1 | Gửi request hợp lệ | Trả response theo API contract và dữ liệu đúng |
| API-002 | P1 | Thiếu field bắt buộc | Validation từ chối request; error envelope nhất quán |
| API-003 | P1 | Field null/blank/whitespace | Xử lý đúng theo validation/normalization đã khai báo |
| API-004 | P1 | Sai kiểu dữ liệu/JSON malformed | Trả lỗi client có kiểm soát; không phát sinh lỗi 500 ngoài dự kiến |
| API-005 | P2 | Field vượt min/max/precision | Bị từ chối hoặc xử lý theo contract; dữ liệu không bị lưu sai |
| API-006 | P2 | Unicode, dấu tiếng Việt, ký tự đặc biệt | Dữ liệu hợp lệ được lưu/đọc an toàn; không tạo SQL injection |
| API-007 | P1 | ID không tồn tại | Kết quả not-found theo error contract |
| API-008 | P1 | Tạo dữ liệu trùng unique key | Không tạo bản ghi trùng; lỗi theo contract |
| API-009 | P1 | Business conflict/state không hợp lệ | Không thực hiện side effect; trả lỗi theo contract |
| API-010 | P2 | Danh sách rỗng, filter, sort, pagination | Kết quả đúng điều kiện, thứ tự và metadata |
| SEC-001 | P0 | Gọi protected endpoint không có token | Request bị từ chối theo security contract |
| SEC-002 | P0 | Token sai/hết hạn/chữ ký không hợp lệ | Không cấp quyền truy cập |
| SEC-003 | P0 | User có đăng nhập nhưng thiếu authority | Không thực hiện thao tác |
| SEC-004 | P0 | User sửa role/owner/resource ID trong payload | Không thể nâng quyền hoặc truy cập ngoài scope |
| SEC-005 | P1 | Response/log có nguy cơ lộ password/token/secret | Không lộ dữ liệu nhạy cảm |
| DB-001 | P1 | Vi phạm unique/foreign key/not-null | Database giữ được integrity; lỗi được xử lý đúng |
| DB-002 | P1 | Workflow nhiều bước thất bại giữa chừng | Transaction rollback theo yêu cầu |
| DB-003 | P1 | Xóa bản ghi đang được tham chiếu | Bị chặn hoặc xử lý theo delete policy đã duyệt |
| DB-004 | P1 | Hai request đồng thời cập nhật cùng tài nguyên | Không tạo trạng thái mất nhất quán |
| INT-001 | P1 | External service timeout/unavailable | Lỗi được xử lý có kiểm soát; không lưu trạng thái thành công giả |
| INT-002 | P1 | Event/message được gửi lặp | Không nhân đôi side effect nếu nghiệp vụ yêu cầu idempotency |
| FILE-001 | P1 | Upload file rỗng/quá cỡ/sai định dạng | Bị từ chối theo giới hạn cấu hình/spec |
| FILE-002 | P1 | Download file không tồn tại/không có quyền | Không lộ file; trả lỗi theo contract |
| FILE-003 | P1 | Filename/path traversal | Không thể đọc/ghi file ngoài storage root |
| JOB-001 | P1 | Scheduled job chạy lại cùng kỳ | Không tạo dữ liệu hoặc side effect trùng nếu job yêu cầu idempotency |

## 4. Domain test-case catalog

Các test dưới đây là ứng viên cần đối chiếu code và business requirement trước khi tự động hóa.

### 4.1 User domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| USER-001 | P1 | Tạo user với dữ liệu hợp lệ | User được tạo; response không chứa credential nhạy cảm |
| USER-002 | P1 | Thiếu/null/blank trường bắt buộc | Validation từ chối; không tạo user |
| USER-003 | P1 | Username/email/identifier bị trùng | Không tạo bản ghi trùng |
| USER-004 | P0 | Lưu và truy xuất password | Chỉ lưu password hash; không lưu/return plaintext |
| USER-005 | P0 | Gán role không được phép qua payload | Không thể tự nâng quyền |
| USER-006 | P1 | User inactive/locked đăng nhập | Đăng nhập bị xử lý theo account policy hiện hành |
| USER-007 | P1 | User thường đọc/sửa user khác | Bị giới hạn theo ownership/authority |
| USER-008 | P2 | Tìm kiếm, filter, sort và pagination | Trả đúng tập kết quả và thứ tự |
| USER-009 | P1 | Hai request tạo cùng identifier đồng thời | Không tạo duplicate do race condition |
| USER-010 | P1 | Đổi password với credential đầu vào không hợp lệ | Không thay đổi credential hiện tại |

### 4.2 Confirmation domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| CONF-001 | P1 | Tạo yêu cầu xác nhận hợp lệ | Tạo confirmation với trạng thái ban đầu theo spec |
| CONF-002 | P1 | Xác nhận bằng token/code hợp lệ | Chuyển trạng thái đúng và ghi nhận thời điểm |
| CONF-003 | P1 | Token/code sai hoặc không tồn tại | Từ chối; không thay đổi trạng thái nghiệp vụ |
| CONF-004 | P1 | Token/code hết hạn | Từ chối theo expiry policy |
| CONF-005 | P1 | Dùng lại token/code đã sử dụng | Không lặp lại side effect |
| CONF-006 | P0 | User khác xác nhận resource không thuộc scope | Bị từ chối |
| CONF-007 | P1 | Xác nhận lặp hoặc gửi request đồng thời | Xử lý idempotent/conflict theo contract |
| CONF-008 | P2 | SLA/expiry tại ranh giới thời gian | Tính toán đúng với Clock/timezone đã cấu hình |

### 4.3 Partner domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| PART-001 | P1 | Tạo partner với dữ liệu hợp lệ | Partner được lưu và trả đúng thông tin public |
| PART-002 | P1 | Thiếu trường bắt buộc/sai format | Validation từ chối |
| PART-003 | P1 | Partner code hoặc identifier trùng | Không tạo duplicate |
| PART-004 | P1 | Cập nhật partner không tồn tại | Trả not-found theo contract |
| PART-005 | P1 | Xóa/deactivate partner có dữ liệu liên kết | Tuân thủ delete/deactivation policy; không tạo orphan |
| PART-006 | P1 | User không có quyền truy cập partner | Bị từ chối |
| PART-007 | P2 | Tìm kiếm theo code/name/filter | Kết quả đúng và pagination ổn định |
| PART-008 | P1 | Hai request đồng thời cập nhật partner | Không ghi đè/mất dữ liệu trái quy tắc |

### 4.4 Contract domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| CONT-001 | P1 | Tạo contract với partner hợp lệ | Contract được lưu cùng liên kết chính xác |
| CONT-002 | P1 | Partner tham chiếu không tồn tại | Từ chối; không tạo contract mồ côi |
| CONT-003 | P1 | Ngày bắt đầu/kết thúc không hợp lệ | Validation/business rule từ chối |
| CONT-004 | P1 | Contract reference bị trùng | Không tạo duplicate nếu có unique rule |
| CONT-005 | P1 | Chuyển trạng thái contract không hợp lệ | Không đổi trạng thái |
| CONT-006 | P1 | Upload file hợp lệ | File được lưu và metadata đúng |
| CONT-007 | P1 | Upload file sai định dạng/quá giới hạn | Bị từ chối; không để lại file rác |
| CONT-008 | P1 | Storage lỗi trong lúc upload | Không ghi nhận upload thành công giả; cleanup theo thiết kế |
| CONT-009 | P1 | User không đủ quyền download contract | Không trả nội dung file |
| CONT-010 | P2 | Job xử lý contract hết hạn chạy lặp | Không lặp side effect ngoài dự kiến |

### 4.5 Service domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| SERV-001 | P1 | Tạo service hợp lệ | Service được tạo |
| SERV-002 | P1 | Thiếu field hoặc code/name trùng | Validation/unique rule được áp dụng |
| SERV-003 | P1 | Giá trị giá âm/precision/scale không hợp lệ | Xử lý theo quy tắc tiền tệ đã duyệt |
| SERV-004 | P1 | Cập nhật giá service | Giá hiện tại và lịch sử giá đúng theo thiết kế |
| SERV-005 | P1 | Truy vấn giá tại thời điểm lịch sử | Trả đúng effective period |
| SERV-006 | P1 | Service inactive được dùng trong workflow | Cho phép/từ chối theo business rule thực tế |
| SERV-007 | P2 | Filter/sort/pagination service | Danh sách đúng |
| SERV-008 | P1 | Hai request thay đổi giá đồng thời | Không mất lịch sử hoặc ghi nhận sai hiệu lực |

### 4.6 Vehicle domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| VEH-001 | P1 | Tạo vehicle hợp lệ | Vehicle được tạo |
| VEH-002 | P1 | Biển số/identifier sai format hoặc trùng | Validation/unique rule được áp dụng |
| VEH-003 | P1 | Cập nhật vehicle không tồn tại | Trả not-found |
| VEH-004 | P1 | Chuyển trạng thái không hợp lệ | Không cập nhật trạng thái |
| VEH-005 | P1 | Xóa vehicle đang được tham chiếu bởi operation | Tuân thủ delete policy, không orphan |
| VEH-006 | P1 | User truy cập vehicle ngoài scope | Bị từ chối |
| VEH-007 | P2 | Tìm kiếm vehicle theo filter/sort | Trả đúng dữ liệu |
| VEH-008 | P1 | Hai operation đồng thời gán/cập nhật vehicle | Không tạo assignment conflict trái quy tắc |

### 4.7 Yard domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| YARD-001 | P1 | Tạo yard với dữ liệu hợp lệ | Yard được tạo |
| YARD-002 | P1 | Capacity âm/0/ngoài giới hạn | Xử lý theo rule capacity đã xác nhận |
| YARD-003 | P1 | Yard code/name trùng | Không tạo duplicate nếu unique |
| YARD-004 | P1 | Cập nhật yard không tồn tại | Trả not-found |
| YARD-005 | P1 | Xóa yard đang có tài nguyên phụ thuộc | Bị chặn hoặc xử lý theo delete policy |
| YARD-006 | P0 | Hai check-in/allocate đồng thời tại gần đầy capacity | Không vượt capacity nếu invariant yêu cầu |
| YARD-007 | P1 | Check-in/check-out không đúng trạng thái | Không tạo chuyển trạng thái bất hợp lệ |
| YARD-008 | P1 | User truy cập yard ngoài phạm vi được cấp | Bị từ chối |
| YARD-009 | P2 | Inventory/occupancy query | Số liệu khớp dữ liệu nguồn |
| YARD-010 | P1 | Rollback khi allocation workflow thất bại | Không để lại allocation một phần |

### 4.8 Goods domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| GOOD-001 | P1 | Tạo goods hợp lệ | Goods được lưu và liên kết đúng |
| GOOD-002 | P1 | Quantity âm/0/decimal/out-of-range | Validation theo unit và business rule |
| GOOD-003 | P1 | Goods identifier/QR value trùng | Không tạo duplicate |
| GOOD-004 | P1 | Goods tham chiếu trip/vehicle/yard không tồn tại | Từ chối; không tạo orphan |
| GOOD-005 | P1 | Reconcile số lượng khớp manifest | Kết quả reconcile chính xác |
| GOOD-006 | P1 | Reconcile thiếu/thừa/trùng item | Discrepancy được ghi nhận đúng |
| GOOD-007 | P1 | QR token sai chữ ký/hết hạn | Từ chối; không thay đổi dữ liệu goods |
| GOOD-008 | P1 | Bulk request có ID trùng/không tồn tại | Xử lý atomic/partial theo contract |
| GOOD-009 | P0 | Cập nhật quantity đồng thời | Không lost update hoặc âm tồn trái rule |
| GOOD-010 | P1 | User không có quyền xem goods | Bị từ chối |

### 4.9 Tracking domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| TRK-001 | P1 | GPS payload hợp lệ | Telemetry được tiếp nhận và gắn đúng trip/device |
| TRK-002 | P1 | Latitude/longitude thiếu hoặc ngoài range | Bị từ chối |
| TRK-003 | P1 | Timestamp sai format/future/stale | Xử lý theo timestamp policy |
| TRK-004 | P1 | Device/trip không tồn tại | Không gắn telemetry vào sai resource |
| TRK-005 | P1 | Cùng event/message được gửi lặp | Không nhân đôi dữ liệu nếu ingestion idempotent |
| TRK-006 | P1 | GPS event đến sai thứ tự | History/latest tuân thủ ordering rule |
| TRK-007 | P1 | Kafka/MQTT unavailable hoặc consumer lỗi | Retry/recovery/error behavior đúng cấu hình |
| TRK-008 | P1 | Query lịch sử ngoài quyền hoặc trip scope | Bị từ chối |
| TRK-009 | P2 | Geofence boundary/inside/outside | Phân loại vị trí đúng theo hình học và boundary policy |
| TRK-010 | P2 | Retention/cleanup đến hạn | Chỉ xóa dữ liệu đủ điều kiện theo policy |

### 4.10 Finance domain

| ID | Priority | Scenario | Expected result |
|---|---|---|---|
| FIN-001 | P1 | Tạo invoice từ service/order đủ điều kiện | Invoice và liên kết chi tiết đúng |
| FIN-002 | P1 | Tạo invoice từ order không hợp lệ/đã xử lý | Từ chối; không tạo invoice trùng |
| FIN-003 | P0 | Tính tiền với decimal/rounding boundary | Kết quả BigDecimal đúng rounding/scale đã duyệt |
| FIN-004 | P1 | Tính lãi với principal/rate/day boundary | Kết quả khớp công thức được phê duyệt |
| FIN-005 | P1 | Mẫu số bằng 0 hoặc operand null | Không phát sinh arithmetic error không kiểm soát |
| FIN-006 | P1 | Payment hợp lệ | Payment được ghi nhận và balance cập nhật nguyên tử |
| FIN-007 | P0 | Payment/invoice request gửi lặp | Không double-charge/double-post |
| FIN-008 | P1 | Payment vượt số dư hoặc số tiền âm | Xử lý theo finance rule đã xác nhận |
| FIN-009 | P1 | Cập nhật trạng thái invoice sai transition | Bị từ chối |
| FIN-010 | P1 | Scheduled interest calculation chạy lặp | Không ghi nhận lãi trùng |
| FIN-011 | P1 | E-invoice provider timeout/error | Lưu trạng thái lỗi/retry theo integration contract; không giả lập thành công |
| FIN-012 | P1 | Export invoice PDF/XLSX | Đúng content type, file content, tên file và quyền truy cập |
| FIN-013 | P1 | Report tổng hợp theo khoảng ngày | Số liệu khớp nguồn và timezone/filter |
| FIN-014 | P1 | Transaction lỗi giữa invoice/payment workflow | Rollback theo transaction boundary |
| FIN-015 | P0 | Hai payment đồng thời cho cùng invoice | Balance/status không bị cập nhật sai hoặc vượt invariant |

## 5. Traceability matrix

Mỗi yêu cầu thay đổi phải có liên kết hai chiều:

| Requirement / AC | Test Case IDs | Automated test class/method | Status |
|---|---|---|---|
| AC-XXX-01 | USER-001, USER-002 | UserServiceTest#... | Planned |
| AC-XXX-02 | SEC-001, SEC-003 | UserControllerSecurityTest#... | Planned |

Status chỉ nhận: Planned, Implemented, Passed, Failed, Blocked, Not Run. Không đánh dấu Passed khi chưa thực thi.

## 6. Test-case specification template

Dùng template sau khi tạo test-case chi tiết cho một ticket/domain:

```markdown
### [TEST-ID] — [Tên scenario]

- Requirement ID:
- Domain/Feature:
- Priority:
- Type:
- Test level:
- Preconditions:
- Test data:
- Steps:
  1.
  2.
- Expected result:
  - HTTP/API:
  - Database:
  - Side effects/events:
  - Security:
- Automation:
- Execution command:
- Result: Planned
- Evidence:
- Notes/assumptions:
```

## 7. Liên kết với automated tests hiện có

Tại thời điểm rà soát catalog, repository có `yms-service/src/test/java/com/yms/yms/PostgresDatabaseIntegrationTest.java`. Test này kiểm tra kết nối PostgreSQL/Testcontainers, persistence cho Partner và ReportSchedule, và API tạo Partner. Đây là integration coverage ban đầu, **không đồng nghĩa 10 domain đã có unit/API/security test đầy đủ**.

Khi bổ sung test:
- Đặt class trong package domain tương ứng dưới `yms-service/src/test/java/com/yms/yms/domain/<domain>/` nếu phù hợp với package production.
- Không di chuyển test hiện hữu nếu không cần thiết.
- Với PostgreSQL-specific behavior, dùng Testcontainers PostgreSQL.
- Giữ test hiện có và bổ sung regression test theo từng feature.

## 8. Quy tắc duy trì

- Thêm test-case khi có feature, bug, security issue hoặc business rule mới.
- Mỗi test tự động phải truy ngược được về requirement hoặc bug ID.
- Khi requirement đổi, cập nhật AC, test-case, test code và tài liệu domain cùng một thay đổi.
- Định kỳ loại bỏ duplicate test-case nhưng phải giữ regression intent.
- Không biến checklist thành khẳng định rằng chức năng đã được triển khai.
