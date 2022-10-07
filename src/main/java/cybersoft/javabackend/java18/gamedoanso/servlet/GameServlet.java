package cybersoft.javabackend.java18.gamedoanso.servlet;

import cybersoft.javabackend.java18.gamedoanso.Game;
import cybersoft.javabackend.java18.gamedoanso.model.GameSession;
import cybersoft.javabackend.java18.gamedoanso.model.Guess;
import cybersoft.javabackend.java18.gamedoanso.model.Player;
import cybersoft.javabackend.java18.gamedoanso.service.GameService;
import cybersoft.javabackend.java18.gamedoanso.utils.JspUtils;
import cybersoft.javabackend.java18.gamedoanso.utils.UrlUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "gameServlet", urlPatterns = {
        UrlUtils.GAME,
        UrlUtils.NEW_GAME,
        UrlUtils.XEP_HANG,
        UrlUtils.LICH_SU
})
public class GameServlet extends HttpServlet {
    private GameService gameService;

    // init -> service -> destroy
    @Override
    public void init() throws ServletException {
        super.init();
        gameService = GameService.getINSTANCE();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case UrlUtils.GAME, UrlUtils.NEW_GAME -> loadGame(req, resp);
            case UrlUtils.XEP_HANG -> rankGame(req, resp);
            case UrlUtils.LICH_SU -> historyGame(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + UrlUtils.NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case UrlUtils.GAME -> processGame(req, resp);
            case UrlUtils.NEW_GAME -> processNewGame(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + UrlUtils.NOT_FOUND);
        }
    }

    private void historyGame(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var currentUser = (Player) req.getSession().getAttribute("currentUser");
        List<GameSession> historyGames = gameService.getHistory(currentUser.getUsername());
        req.setAttribute("historyGames", historyGames);

        // neu url co id game session tro ve game cu
        String gameSSID = req.getParameter("id");
        if(gameSSID != null){
            loadOldGame(req,resp,gameSSID);
        }

        req.getRequestDispatcher(JspUtils.LICH_SU)
                .forward(req, resp);
    }

    // load game cu
    private void loadOldGame(HttpServletRequest req, HttpServletResponse resp, String gameSSID) throws ServletException, IOException {
        var currentUser = (Player) req.getSession().getAttribute("currentUser");
        GameSession game = gameService.getOldGame(currentUser.getUsername(),gameSSID);
        req.setAttribute("game", game);
        req.getRequestDispatcher(JspUtils.GAME)
             .forward(req, resp);
    }


    private void rankGame(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //lay ra danh sach hoan thanh game
        List<GameSession> listRank = gameService.getListRank();
        req.setAttribute("listRank", listRank);
        req.getRequestDispatcher(JspUtils.XEP_HANG)
                .forward(req, resp);
    }


    // load game hien tai hoac game moi
    private void loadGame(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var currentUser = (Player) req.getSession().getAttribute("currentUser");
        // create new game/get existed game
        GameSession game = gameService.getCurrentGame(currentUser.getUsername());
        // put in req
        req.setAttribute("game", game);
        req.getRequestDispatcher(JspUtils.GAME)
                .forward(req, resp);
    }


    private void processNewGame(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var currentUser = (Player) req.getSession().getAttribute("currentUser");
        // create new game/get existed game
        gameService.skipAndPlayNewGame(currentUser.getUsername());

        resp.sendRedirect(req.getContextPath() + UrlUtils.GAME);
    }

    private void processGame(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String gameSessionId = req.getParameter("game-session");
        int guessNumber = Integer.parseInt(req.getParameter("guess"));

        var gameSession = gameService.getGameSession(gameSessionId);

        if (gameSession == null) { // if the session is not existed, ask the player to sign in again
            req.getSession().invalidate();
            resp.sendRedirect(req.getContextPath() + UrlUtils.DANG_NHAP);
            return;
        }


        gameSession.getGuess().add(createGuess(gameSession, guessNumber));

        if (guessNumber == gameSession.getTargetNumber()) {
            var endTime = gameSession
                    .getGuess().stream()
                    .filter(r -> r.getResult() == 0)
                    .map(Guess::getTimestamp).findFirst().get();
            gameService.completeGame(gameSessionId,endTime);
        }

        resp.sendRedirect(req.getContextPath() + UrlUtils.GAME);
    }

    private Guess createGuess(GameSession gameSession, int guessNumber) {
        int result = Integer.compare(guessNumber, gameSession.getTargetNumber());
        Guess newGuess = new Guess(guessNumber, gameSession.getId(), result);
        gameService.saveGuess(newGuess);
        return newGuess;
    }
}
