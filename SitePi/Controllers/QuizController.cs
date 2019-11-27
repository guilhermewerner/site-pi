using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using SitePi.Models;

namespace SitePi.Controllers
{
    public class QuizController : Controller
    {
        private AppContext AppContext;

        private int AmountA = 0;
        private int AmountB = 0;
        private int AmountC = 0;
        private int AmountD = 0;

        private int QuestionsCount = 0;

        public QuizController(AppContext AppContext)
        {
            this.AppContext = AppContext;
        }

        public IActionResult Index()
        {
            Quiz Current = AppContext.Quizzes.Find(1);
            
            ViewBag.Users = AppContext.Users;

            ViewData["QuizTitle"] = Current.Name;

            ViewBag.Questions = AppContext.Questions.Where(q => q.QuizId == 1);
            
            ViewBag.Choices = AppContext.Choices;

            return View();
        }

        [HttpPost]
        public IActionResult Index(Responds rep)
        {
            User Logged = AppContext.Users.Find(rep.UserId);
            int UserId = rep.UserId;

            Quiz Current = AppContext.Quizzes.Find(1);
            int QuizId = AppContext.Quizzes.Find(1).Id;

            ViewBag.Questions = AppContext.Questions.Where(q => q.QuizId == 1);

            foreach (Question q in ViewBag.Questions)
            {
                QuestionsCount++;
            }

            rep.UserId = UserId;
            rep.User = Logged;

            rep.QuizId = QuizId;
            rep.Quiz = Current;

            Check(rep.Profile_1);
            Check(rep.Profile_2);
            Check(rep.Profile_3);
            Check(rep.Profile_4);
            Check(rep.Profile_5);
            Check(rep.Profile_6);
            Check(rep.Profile_7);

            int PercentageA = (AmountA * 100) / 7;     //Criatividade
            int PercentageB = (AmountB * 100) / 7;     //Liderança
            int PercentageC = (AmountC * 100) / 7;     //Individualista
            int PercentageD = (AmountD * 100) / 7;     //Insegurança

            string _Name = "";
            string _FileBytes = "";
            string _FileName = "";

            if (PercentageA >= 50)
            {
                _Name = "Responsive";
                _FileBytes = @".\wwwroot\uploads\HunterBot.java";
                _FileName = "ResponsiveBot.java";
            }
            else if (PercentageB >= 50)
            {
                _Name = "Communicating";
                _FileBytes = @".\wwwroot\uploads\HunterBot.java";
                _FileName = "CommunicatingBot.java";
            }
            else if (PercentageC >= 50)
            {
                _Name = "Hunter";
                _FileBytes = @".\wwwroot\uploads\HunterBot.java";
                _FileName = "HunterBot.java";
            }
            else
            {
                _Name = "Hide";
                _FileBytes = @".\wwwroot\uploads\HunterBot.java";
                _FileName = "HideBot.java";
            }

            Bot bot = new Bot
            {
                Name = _Name,

                FileBytes = _FileBytes,
                FileName = _FileName,

                User = Logged,
                UserId = Logged.Id,

                PercentageA = PercentageA,
                PercentageB = PercentageB,
                PercentageC = PercentageC,
                PercentageD = PercentageD
            };

            if (ModelState.IsValid)
            {
                AppContext.Bots.Add(bot);
                AppContext.SaveChanges();
                
                return RedirectToAction("Index", "Home");
            }

            return View("Index", Response);
        }
        
        public void Check(string choice)
        {
            switch (choice)
            {
                case "a":
                    AmountA++;
                    break;
                case "b":
                    AmountB++;
                    break;
                case "c":
                    AmountC++;
                    break;
                case "d":
                    AmountD++;
                    break;
                default:
                    AmountD++;
                    break;
            }
        }
    }
}