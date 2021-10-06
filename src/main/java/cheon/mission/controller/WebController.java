package cheon.mission.controller;

import cheon.mission.auth.Dto.SessionUser;
import cheon.mission.domain.Dto.JoinMissionListDto;
import cheon.mission.domain.Dto.Message;
import cheon.mission.domain.Dto.MissionDto;
import cheon.mission.domain.Dto.Participant;
import cheon.mission.domain.Mission;
import cheon.mission.domain.MissionStatus;
import cheon.mission.domain.User;
import cheon.mission.domain.UserMission;
import cheon.mission.service.MissionService;
import cheon.mission.service.UserMissionService;
import cheon.mission.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Controller
@SessionAttributes("user")
public class WebController {

    private final HttpSession httpSession;
    private final MissionService missionService;
    private final UserService userService;
    private final UserMissionService userMissionService;


    @GetMapping("/")
    public String home(Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getName());
            model.addAttribute("useremail", user.getEmail());
        }

        List<Mission> allMissions = missionService.findAll();

        model.addAttribute("allMissions", allMissions);

        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam("search") String search, Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getName());
            model.addAttribute("useremail", user.getEmail());
        }

        List<Mission> allMissions = missionService.findMissions(search);

        model.addAttribute("search", search);
        model.addAttribute("allMissions", allMissions);

        return "search";
    }


    @GetMapping("/login_page")
    public String login() {
        return "login_page";
    }

    @GetMapping("/register")
    public String register(Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getName());
            model.addAttribute("useremail", user.getEmail());
        }

        model.addAttribute("missionDto", new MissionDto());

        return "register";
    }

    @PostMapping("/register")
    public String registerMission(MissionDto missionDto) {
        LocalDate startDate = LocalDate.parse(missionDto.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(missionDto.getEndDate(), DateTimeFormatter.ISO_DATE);

        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");

        User user = userService.findByEmail(sessionUser.getEmail());

        Mission mission = new Mission(missionDto.getName(),
                missionDto.getContext(),
                startDate,
                endDate,
                missionDto.getCode(),
                MissionStatus.PROGRESS);

        mission.setOwner(user);

        UserMission userMission = new UserMission(LocalDateTime.now());
        userMission.setMissionUser(mission);
        userMission.setUserMission(user);

        missionService.save(mission);
        userMissionService.save(userMission);

        return "redirect:/";
    }

    @GetMapping("/password")
    public String password() {
        return "password";
    }

    @GetMapping("/mission/{missionId}")
    public String missionDetail(@PathVariable("missionId") Long missionId, Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getName());
            model.addAttribute("useremail", user.getEmail());
        }
        List<Participant> userList = userMissionService.findByMissionId(missionId);
        Mission mission = missionService.findById(missionId);

        model.addAttribute("userList", userList);
        model.addAttribute("mission", mission);

        return "mission";
    }

    @PostMapping("/mission/{missionId}")
    public ModelAndView missionApplication(@PathVariable("missionId") Long missionId, ModelAndView modelAndView) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        User findUser = userService.findByEmail(user.getEmail());
        Mission findMission = missionService.findById(missionId);

        UserMission userMission = new UserMission(LocalDateTime.now());
        userMission.setUserMission(findUser);
        userMission.setMissionUser(findMission);


        try {
            userMissionService.save(userMission);
            modelAndView.addObject("data", new Message("미션이 추가되었습니다.", "/mission/" + missionId));
            modelAndView.setViewName("message");

        } catch (IllegalStateException e) {

            modelAndView.addObject("data", new Message(e.getMessage(), "/mission/" + missionId));
            modelAndView.setViewName("message");
        }

        return modelAndView;
    }

    @GetMapping("/mission/management")
    public String missionManagement(Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        List<JoinMissionListDto> missionList = userMissionService.findJoinMissionByUserId(user);
        List<Mission> myMissionList = missionService.findByUserId(user);

        model.addAttribute("username", user.getName());
        model.addAttribute("useremail", user.getEmail());
        model.addAttribute("missionList", missionList);
        model.addAttribute("myMissionList", myMissionList);

        return "missionManagement";
    }

    @DeleteMapping("/mission/join/delete/{missionId}")
    public String joinedMissionManagement(@PathVariable("missionId") Long missionId) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        userMissionService.deleteUserMission(user, missionId);


        return "redirect:/mission/management";
    }

    @DeleteMapping("/mission/create/delete/{missionId}")
    public String createdMissionManagement(@PathVariable("missionId") Long missionId) {
        missionService.deleteMission(missionId);

        return "redirect:/mission/management";
    }

    @GetMapping("/tables")
    public String tables(Model model) {
        Long missionId = 2L;
        List<Participant> userList = userMissionService.findByMissionId(missionId);
        Mission mission = missionService.findById(missionId);

        model.addAttribute("userList", userList);
        model.addAttribute("mission", mission);
        return "tables";
    }

}
