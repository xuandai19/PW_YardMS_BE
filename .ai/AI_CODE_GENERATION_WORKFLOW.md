# AI Code Generation & Testing Workflow

## Mục tiêu

Quy định quy trình bắt buộc cho AI agent khi phân tích yêu cầu, sinh mới/chỉnh sửa backend, tạo test-case, thực thi kiểm thử và báo cáo kết quả trong `PW_YardMS_BE`.

Tài liệu này bổ sung cho:
- `AGENTS.md`
- `.ai/AI_INSTRUCTIONS.md`
- `.ai/AI_TESTING_GUIDELINES.md`
- `.ai/TEST_CASE_CATALOG.md`
- `.ai/DOMAIN_BUSINESS_RULES.md`
- `.ai/CODING_STANDARDS.md`
- `.ai/ARCHITECTURE_GUIDELINES.md`
- `.ai/API_DESIGN_GUIDELINES.md`
- `.ai/DATABASE_GUIDELINES.md`
- `.ai/SECURITY_GUIDELINES.md`
- `.ai/GIT_WORKFLOW.md`

## 1. Nguyên tắc bắt buộc

1. Đọc `AGENTS.md` và các tài liệu liên quan trước khi thay đổi.
2. Xác định yêu cầu từ ticket/spec/acceptance criteria; không tự suy diễn nghiệp vụ.
3. Kiểm tra code, DTO, entity, repository, migration, security config, API contract và test hiện tại.
4. Giữ kiến trúc package-by-domain và quy ước hiện có.
5. Thay đổi production code và test trong cùng một nhánh công việc.
6. Không xóa, vô hiệu hóa, làm yếu assertion hoặc sửa test chỉ để build xanh.
7. Không gọi hệ thống bên ngoài thật trong unit test.
8. Không ghi secret, token, dữ liệu cá nhân/nhạy cảm hoặc dữ liệu production vào test fixture/log.
9. Không tuyên bố test đã chạy thành công nếu chưa chạy và kiểm tra kết quả.
10. Nếu thiếu yêu cầu hoặc có xung đột giữa spec và code, ghi rõ câu hỏi/giả định; không tự chọn một quy tắc nghiệp vụ.

## 2. Quy trình thực hiện

### Phase 0 — Khảo sát repository

- Kiểm tra branch, working tree, commit gần nhất và cấu trúc module.
- Xác định Java/Maven/dependency version từ POM; không dựa riêng vào README.
- Tìm implementation tương tự để tái sử dụng convention.
- Xác định test hiện có và cách cấu hình test profile.
- Ghi nhận phạm vi file dự kiến thay đổi.

**Output:** danh sách domain, class, API, DB object, integration và test bị ảnh hưởng.

### Phase 1 — Phân tích yêu cầu

Chuyển yêu cầu thành acceptance criteria có thể kiểm chứng:

| ID | Requirement | Preconditions | Action | Expected observable result | Source |
|---|---|---|---|---|---|
| AC-XXX-01 | Mô tả yêu cầu đã xác nhận | Trạng thái/dữ liệu ban đầu | Hành động | Kết quả quan sát được | Ticket/spec |

- Mỗi tiêu chí phải có nguồn hoặc được đánh dấu là câu hỏi cần xác nhận.
- Xác định actor, quyền truy cập, resource scope và trạng thái dữ liệu.
- Xác định validation, boundary, side effect, transaction và integration.
- Phân biệt backend behavior với UI-only behavior.

### Phase 2 — Lập test design trước khi code

1. Chọn test-case từ `.ai/TEST_CASE_CATALOG.md`.
2. Áp dụng checklist trong `.ai/AI_TESTING_GUIDELINES.md`.
3. Bổ sung test riêng cho acceptance criteria và bug regression.
4. Đánh dấu N/A kèm lý do; không bỏ qua im lặng.
5. Xác định test level: unit, repository, API/security, integration hoặc async.
6. Xác định dữ liệu đầu vào, setup, expected result và cleanup.

**Output:** danh sách test-case ID và test level tương ứng.

### Phase 3 — Sinh/chỉnh sửa code

- Đặt code trong đúng domain và đúng layer.
- Controller chỉ xử lý HTTP mapping và delegate.
- Business rule thuộc service/domain layer.
- Persistence thuộc repository.
- External system đi qua client/adapter.
- Sử dụng DTO ở API boundary theo convention hiện có.
- Dùng validation ở boundary và bảo vệ invariant trong business layer.
- Dùng transaction cho thao tác nhiều bước cần tính nguyên tử.
- Dùng `BigDecimal` cho tiền; xử lý thời gian theo timezone/Clock rõ ràng.
- Không thêm dependency, abstraction hoặc refactor ngoài phạm vi nếu không có lý do.
- Không sửa migration đã được áp dụng; tạo migration mới theo quy tắc database của dự án.

### Phase 4 — Sinh test tự động

- Đặt test theo package/domain tương ứng dưới `yms-service/src/test/java/com/yms/yms/`.
- Dùng JUnit 5 và Mockito cho unit test; Spring Boot Test/MockMvc cho API; Spring Security Test cho quyền; Testcontainers PostgreSQL cho hành vi phụ thuộc PostgreSQL.
- Dùng fixture có tính xác định; không phụ thuộc thứ tự chạy.
- Kiểm tra cả kết quả và side effect quan trọng.
- Với lỗi, xác nhận HTTP status, error envelope và field error theo contract thực tế.
- Mock mail, storage, Kafka, MQTT và external provider trong test thông thường.
- Không dùng `Thread.sleep()` để đồng bộ test bất đồng bộ.
- Không giả định H2 tương đương PostgreSQL đối với query/constraint/extension đặc thù.

### Phase 5 — Thực thi kiểm thử

Chạy từ repository root, lựa chọn theo phạm vi:

```bash
mvn -pl yms-service -am -DskipTests=false test
mvn -pl yms-service -Dtest=YourTestClass test
mvn -Pcoverage -pl yms-service -am verify
```

PostgreSQL integration test (cần Docker):

```bash
mvn -pl yms-service -Dtest=PostgresDatabaseIntegrationTest test
```

- Chạy test hẹp trước, sau đó chạy suite rộng phù hợp.
- Kiểm tra exit code, Maven summary và `yms-service/target/surefire-reports/`.
- Nếu Docker/dependency/service không khả dụng, ghi **NOT RUN** cùng nguyên nhân.
- Nếu test fail, phân loại lỗi: regression do change, lỗi có sẵn, lỗi môi trường hoặc test không ổn định.
- Không thay test bằng manual check rồi báo automated test passed.

### Phase 6 — Review trước khi kết thúc

Kiểm tra:
- [ ] Không có thay đổi ngoài phạm vi.
- [ ] Không có secret hoặc dữ liệu nhạy cảm.
- [ ] Không bỏ qua validation/security/transaction.
- [ ] Không phá vỡ API contract nếu chưa được phê duyệt.
- [ ] Test bao phủ success, failure, boundary và side effect phù hợp.
- [ ] Test không phụ thuộc dữ liệu production hoặc external service thật.
- [ ] Migration tương thích và không sửa migration đã áp dụng.
- [ ] Tài liệu liên quan đã được cập nhật.
- [ ] Diff đã được đọc lại.

## 3. Quy tắc quyết định khi thiếu thông tin

| Tình huống | Cách xử lý |
|---|---|
| Không rõ field bắt buộc/độ dài/format | Kiểm tra DTO, validator, migration và spec; nếu vẫn thiếu thì hỏi, không tự đặt giới hạn |
| Không rõ role được phép thao tác | Kiểm tra SecurityConfig, method security, service và business spec; không suy luận từ tên role |
| Không rõ status transition | Tìm state machine/enum/service/tests; ghi câu hỏi nếu không có nguồn xác nhận |
| Spec khác implementation | Báo discrepancy; không tự thay đổi behavior |
| Thiếu expected HTTP status/error schema | Theo contract hiện có; nếu không xác định được thì yêu cầu xác nhận |
| Không thể chạy test | Ghi NOT RUN và nguyên nhân; không kết luận pass |
| Test hiện có fail trước thay đổi | Ghi baseline failure và bằng chứng; không xóa/skip test |

## 4. Format báo cáo bắt buộc

Mọi task hoàn thành phải báo cáo:

```text
1. Summary
   - Yêu cầu:
   - Domain bị ảnh hưởng:
   - Hành vi thay đổi:

2. Files changed
   - Production:
   - Tests:
   - Documentation/migration:

3. Test design
   - Test-case IDs:
   - Unit:
   - API/security:
   - Repository/integration:
   - N/A và lý do:

4. Test execution
   - Command:
   - PASS:
   - FAIL:
   - NOT RUN:
   - Evidence/report path:

5. Review
   - Security/API/DB:
   - Regression risk:
   - Assumptions/questions:
   - Remaining gaps:
```

Chỉ ghi PASS khi lệnh đã thực thi và kết quả được kiểm tra. Phân biệt rõ code đã tạo với code đã build/test thành công.

## 5. Definition of Done

- [ ] Yêu cầu và acceptance criteria đã được xác định.
- [ ] Đã đọc đúng tài liệu AI và code liên quan.
- [ ] Production code tuân thủ kiến trúc và coding standards.
- [ ] Test-case đã được lập và trace về requirement.
- [ ] Test tự động đã được thêm/cập nhật.
- [ ] Test phù hợp đã được chạy; phần chưa chạy được ghi rõ.
- [ ] Diff và security implications đã được review.
- [ ] Báo cáo có file, command, kết quả và rủi ro.
