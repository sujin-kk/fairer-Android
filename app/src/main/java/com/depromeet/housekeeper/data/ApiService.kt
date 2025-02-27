package com.depromeet.housekeeper.data

import com.depromeet.housekeeper.model.request.*
import com.depromeet.housekeeper.model.response.*
import retrofit2.http.*

interface ApiService {

    /**
     * houseWorks Complete
     */
    @DELETE("/api/houseworks/complete/{houseWorkCompleteId}")
    suspend fun deleteChoreComplete(
        @Path("houseWorkCompleteId") houseWorkId:Int
    )

    @POST("/api/houseworks/complete/{houseWorkId}")
    suspend fun updateChoreState(
        @Path("houseWorkId") houseWorkId: Int,
        @Query("scheduledDate") scheduledDate: String,
    ): UpdateChoreResponse


    /**
     * houseWorks
     */
    @POST("/api/houseworks")
    suspend fun createHouseWorks(@Body houseWorks: List<Chore>): List<HouseWork>


    @GET("/api/houseworks/{houseWorkId}/detail")
    suspend fun getDetailHouseWork(@Path("houseWorkId") houseWorkId: Int): HouseWork

    @GET("/api/houseworks/list/member/{teamMemberId}/query")
    suspend fun getPeriodHouseWorkListOfMember(
        @Path("teamMemberId") teamMemberId: Int,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
    ): Map<String, HouseWorks> // ex) key: 2022-08-14

    @GET("/api/houseworks/list/query")
    suspend fun getDateHouseWorkList(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String
    ): Map<String, HouseWorks>

    @GET("/api/houseworks/success/count")
    suspend fun getCompletedHouseWorkNumber(@Query("scheduledDate") scheduledDate: String): CompleteHouseWork

    @PUT("/api/houseworks/v2")
    suspend fun editHouseWork(@Body editChore: EditChore)

    @HTTP(method = "DELETE", path="/api/houseworks/v2", hasBody = true)
    suspend fun deleteHouseWork(@Body deleteChore: DeleteChoreRequest)


    /**
     * members
     */
    @GET("/api/member/profile-image")
    suspend fun getProfileImages(): ProfileImages

    @PATCH("/api/member")
    suspend fun updateMember(@Body updateMember: UpdateMember): UpdateMemberResponse

    @GET("/api/member/me")
    suspend fun getMe(): ProfileData

    @PATCH("/api/member")
    suspend fun updateMe(@Body editProfileModel: EditProfileModel): EditResponseBody


    /**
     * oauth
     */
    @POST("/api/oauth/login")
    suspend fun googlelogin(@Body socialType: SocialType): LoginResponse

    @POST("/api/oauth/logout")
    suspend fun logout()


    /**
     * presets
     */
    @GET("/api/preset")
    suspend fun getChoreList(): List<ChoreList>


    /**
     * rules
     */
    @POST("/api/rules")
    suspend fun createRules(@Body rule: Rule): RuleResponses

    @GET("/api/rules")
    suspend fun getRules(): RuleResponses

    @DELETE("/api/rules/{ruleId}")
    suspend fun deleteRule(@Path("ruleId") ruleId: Int): Response

    /**
     * statistics 집안일 통계 API
     */
    @GET("/api/statistics") // yearMonth: yyyy-MM
    suspend fun getStatisticsList(@Query("yearMonth") yearMonth: String): StatsListResponse

    @GET("/api/statistics/ranking")
    suspend fun getStatisticsLanking(@Query("month") yearMonth: String): HouseWorkStatsResponse

    @GET("/api/statistics/team-member")
    suspend fun getHouseWorkStatistics(@Query("houseWorkName") houseWorkName: String, @Query("month") month: String): HouseWorkStatsResponse

    /**
     * teams
     */
    @POST("/api/teams")
    suspend fun buildTeam(@Body buildTeam: BuildTeam): BuildTeamResponse

    @GET("/api/teams/my")
    suspend fun getTeamData(): Groups

    @GET("/api/teams/invite-codes")
    suspend fun getInviteCode(): GetInviteCode

    @PATCH("/api/teams")
    suspend fun updateTeam(@Body teamName: BuildTeam): TeamUpdateResponse

    @POST("/api/teams/join")
    suspend fun joinTeam(@Body inviteCode: JoinTeam): JoinTeamResponse

    @POST("/api/teams/leave")
    suspend fun leaveTeam()


    /**
    fcm
     */
    @POST("/api/fcm/token")
    suspend fun saveToken(@Body token: Token)

    @POST("/api/fcm/message")
    suspend fun sendMessage(@Body message: Message): Message

}