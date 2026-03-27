package com.healthcare.portal.controller;

import com.healthcare.portal.dto.*;
import com.healthcare.portal.entity.BuyerVerification;
import com.healthcare.portal.entity.User;
import com.healthcare.portal.service.AdminService;
import com.healthcare.portal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin panel management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final AuthService authService;

    @Autowired
    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats", description = "Returns dashboard statistics")
    public ResponseEntity<ApiResponse<AdminDTO.DashboardStats>> getDashboardStats() {
        AdminDTO.DashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @GetMapping("/listings/stats")
    @Operation(summary = "Get listing statistics", description = "Returns listing statistics")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getListingStats() {
        java.util.Map<String, Object> stats = adminService.getListingStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @GetMapping("/users/stats")
    @Operation(summary = "Get user statistics", description = "Returns user statistics")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getUserStats() {
        java.util.Map<String, Object> stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    // Listing Management
    
    @GetMapping("/listings/{id}")
    @Operation(summary = "Get listing by id", description = "Returns full details of a listing")
    public ResponseEntity<ApiResponse<ListingDTO.ListingResponse>> getListingById(@PathVariable Long id) {
        ListingDTO.ListingResponse response = adminService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/listings")
    @Operation(summary = "Get all listings", description = "Returns all listings with optional status filter")
    public ResponseEntity<ApiResponse<PageResponse<ListingDTO.ListingSummaryResponse>>> getAllListings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long dealTypeId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long stateId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ListingDTO.ListingSummaryResponse> response = 
                adminService.getAllListings(page, size, status, categoryId, dealTypeId, cityId, stateId, sellerId, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/listings/{id}/approve")
    @Operation(summary = "Approve listing", description = "Approves a pending listing")
    public ResponseEntity<ApiResponse<Void>> approveListing(@PathVariable Long id) {
        adminService.approveListing(id);
        return ResponseEntity.ok(ApiResponse.success("Listing approved successfully", null));
    }
    
    @PostMapping("/listings/{id}/reject")
    @Operation(summary = "Reject listing", description = "Rejects a pending listing")
    public ResponseEntity<ApiResponse<Void>> rejectListing(
            @PathVariable Long id,
            @RequestBody AdminDTO.ListingApprovalRequest request) {
        adminService.rejectListing(id, request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Listing rejected", null));
    }
    
    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Delete listing", description = "Deletes a listing")
    public ResponseEntity<ApiResponse<Void>> deleteListing(@PathVariable Long id) {
        adminService.deleteListing(id);
        return ResponseEntity.ok(ApiResponse.success("Listing deleted successfully", null));
    }
    
    @PostMapping("/listings/{id}/feature")
    @Operation(summary = "Feature listing", description = "Marks a listing as featured")
    public ResponseEntity<ApiResponse<Void>> featureListing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days) {
        adminService.featureListing(id, days);
        return ResponseEntity.ok(ApiResponse.success("Listing featured successfully", null));
    }
    
    @PutMapping("/listings/{id}")
    @Operation(summary = "Update listing", description = "Updates listing details")
    public ResponseEntity<ApiResponse<Void>> updateListing(
            @PathVariable Long id,
            @RequestBody ListingDTO.ListingRequest request) {
        adminService.updateListing(id, request);
        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", null));
    }
    
    // Job Management
    
    @GetMapping("/jobs")
    @Operation(summary = "Get all jobs", description = "Returns all jobs with optional status filter")
    public ResponseEntity<ApiResponse<PageResponse<JobDTO.JobSummaryResponse>>> getAllJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<JobDTO.JobSummaryResponse> response = adminService.getAllJobs(
                page, size, status, categoryId, cityId, employmentType, experienceLevel, employerId, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/jobs/{id}/approve")
    @Operation(summary = "Approve job", description = "Approves a pending job")
    public ResponseEntity<ApiResponse<Void>> approveJob(@PathVariable Long id) {
        adminService.approveJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job approved successfully", null));
    }
    
    @PostMapping("/jobs/{id}/reject")
    @Operation(summary = "Reject job", description = "Rejects a pending job")
    public ResponseEntity<ApiResponse<Void>> rejectJob(
            @PathVariable Long id,
            @RequestBody AdminDTO.JobApprovalRequest request) {
        adminService.rejectJob(id, request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Job rejected", null));
    }
    
    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete job", description = "Deletes a job")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        adminService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
    }
    
    // User Management
    
    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Returns all users with filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO.UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<UserDTO.UserResponse> response = adminService.getAllUsers(page, size, keyword, role, status, isVerified);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details", description = "Returns full details and activity counts for a user")
    public ResponseEntity<ApiResponse<AdminDTO.UserDetailResponse>> getUserDetails(@PathVariable Long userId) {
        AdminDTO.UserDetailResponse response = adminService.getUserDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend user", description = "Suspends a user account")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @RequestBody AdminDTO.UserStatusRequest request) {
        adminService.suspendUser(userId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("User suspended successfully", null));
    }
    
    @PostMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activates a suspended user account")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", null));
    }
    
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Deletes a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
    
    // Verification Management
    
    @GetMapping("/verifications")
    @Operation(summary = "Get pending verifications", description = "Returns pending buyer verification requests")
    public ResponseEntity<ApiResponse<PageResponse<AdminDTO.AdminVerificationResponse>>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AdminDTO.AdminVerificationResponse> response = adminService.getPendingVerifications(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/verifications/{id}/approve")
    @Operation(summary = "Approve verification", description = "Approves a buyer verification request")
    public ResponseEntity<ApiResponse<Void>> approveVerification(@PathVariable Long id) {
        User currentUser = authService.getCurrentUserEntity();
        adminService.approveVerification(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Verification approved successfully", null));
    }
    
    @PostMapping("/verifications/{id}/reject")
    @Operation(summary = "Reject verification", description = "Rejects a buyer verification request")
    public ResponseEntity<ApiResponse<Void>> rejectVerification(
            @PathVariable Long id,
            @RequestBody AdminDTO.VerificationApprovalRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        adminService.rejectVerification(id, currentUser.getId(), request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Verification rejected", null));
    }
}
